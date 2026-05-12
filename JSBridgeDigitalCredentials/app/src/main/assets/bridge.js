(function() {
    if (!navigator.credentials) {
        navigator.credentials = {};
    }
    const originalGet = navigator.credentials.get;
    navigator.credentials.get = async function(options) {
        console.log("navigator.credentials.get called with options:", JSON.stringify(options));
        if (options && options.digital) {
            console.log("Interception Digital Credentials request");
            if (window.DigitalCredentialBridge) {
                return new Promise((resolve, reject) => {
                    const digital = options.digital;
                    if (digital.providers && digital.providers.length > 0) {
                        const provider = digital.providers[0];

                        if (provider.protocol === "openid4vp") {
                            // In your index.html, 'request' is already a stringified JSON
                            const requestJson = provider.request;
                            const message = {
                                type: "get",
                                request: requestJson // Send ONLY the inner request string
                            };
                            console.log("Sending message to native bridge:", JSON.stringify(message));
                            window.DigitalCredentialBridge.postMessage(JSON.stringify(message));
                            window.DigitalCredentialBridge.onmessage = function(event) {
                                console.log("Received response from native bridge:", event.data);
                                try {
                                    const response = JSON.parse(event.data);
                                    if (response.error) {
                                        reject(new Error(response.error));
                                    } else {
                                        resolve(response);
                                    }
                                } catch (e) {
                                    // If it's not JSON, just resolve with the raw data
                                    resolve(event.data);
                                }
                            };
                        } else {
                            reject(new Error("Unsupported protocol: " + provider.protocol));
                        }
                    } else {
                        reject(new Error("No providers specified in digital credential request"));
                    }
                });
            } else {
                console.warn("DigitalCredentialBridge not found, falling back to original");
            }
        }
        if (originalGet) {
            return originalGet.call(navigator.credentials, options);
        } else {
            throw new Error("navigator.credentials.get is not supported and no bridge available");
        }
    };
    console.log("Digital Credentials Bridge initialized");
})();