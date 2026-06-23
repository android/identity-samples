#[derive(Debug, PartialEq)]
pub enum Base64UrlError {
    InvalidCharacter(char),
    InvalidLength,
}

impl std::fmt::Display for Base64UrlError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Base64UrlError::InvalidCharacter(c) => {
                write!(f, "Invalid character in base64url: {}", c)
            }
            Base64UrlError::InvalidLength => write!(f, "Invalid base64url length"),
        }
    }
}

impl std::error::Error for Base64UrlError {}

pub fn decode_base64url(input: &str) -> Result<Vec<u8>, Base64UrlError> {
    let input = input.trim_end_matches('=');
    let mut buffer = Vec::with_capacity((input.len() * 3 + 3) / 4);
    let mut bits = 0u32;
    let mut count = 0;

    for &byte in input.as_bytes() {
        let value = match byte {
            b'A'..=b'Z' => byte - b'A',
            b'a'..=b'z' => byte - b'a' + 26,
            b'0'..=b'9' => byte - b'0' + 52,
            b'-' => 62,
            b'_' => 63,
            _ => return Err(Base64UrlError::InvalidCharacter(byte as char)),
        };

        bits = (bits << 6) | (value as u32);
        count += 6;

        if count >= 8 {
            count -= 8;
            buffer.push(((bits >> count) & 0xFF) as u8);
        }
    }

    if count == 6 {
        return Err(Base64UrlError::InvalidLength);
    }

    Ok(buffer)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_decode_empty() {
        assert_eq!(decode_base64url("").unwrap(), Vec::<u8>::new());
    }

    #[test]
    fn test_decode_simple() {
        // "any carnal pleasure." in base64url
        // Standard: YW55IGNhcm5hbCBwbGVhc3VyZS4=
        // URL-safe NO-PAD: YW55IGNhcm5hbCBwbGVhc3VyZS4
        assert_eq!(
            decode_base64url("YW55IGNhcm5hbCBwbGVhc3VyZS4").unwrap(),
            b"any carnal pleasure."
        );
    }

    #[test]
    fn test_decode_with_padding() {
        // Same as above but with padding
        assert_eq!(
            decode_base64url("YW55IGNhcm5hbCBwbGVhc3VyZS4=").unwrap(),
            b"any carnal pleasure."
        );
    }

    #[test]
    fn test_decode_special_chars() {
        // In base64url, '+' -> '-' and '/' -> '_'
        // Original: b"\xFF\xFE\xFD\xFC" -> //79/A== (standard)
        // URL-safe NO-PAD: __79_A
        assert_eq!(
            decode_base64url("__79_A").unwrap(),
            vec![255, 254, 253, 252]
        );
    }

    #[test]
    fn test_decode_invalid_chars() {
        match decode_base64url("YW55IGNhcm5hbCBwbGVhc3VyZS4+") {
            Err(Base64UrlError::InvalidCharacter('+')) => (),
            _ => panic!("Expected InvalidCharacter('+')"),
        }
    }

    #[test]
    fn test_decode_invalid_length() {
        // Base64url length % 4 == 1 is invalid
        // 'A' is 1 char -> 6 bits, not enough for 1 byte (8 bits).
        match decode_base64url("A") {
            Err(Base64UrlError::InvalidLength) => (),
            _ => panic!("Expected InvalidLength"),
        }
    }

    #[test]
    fn test_decode_all_lengths() {
        // test vectors from RFC 4648
        assert_eq!(decode_base64url("").unwrap(), b"");
        assert_eq!(decode_base64url("Zg").unwrap(), b"f");
        assert_eq!(decode_base64url("Zm8").unwrap(), b"fo");
        assert_eq!(decode_base64url("Zm9v").unwrap(), b"foo");
        assert_eq!(decode_base64url("Zm9vYg").unwrap(), b"foob");
        assert_eq!(decode_base64url("Zm9vYmE").unwrap(), b"fooba");
        assert_eq!(decode_base64url("Zm9vYmFy").unwrap(), b"foobar");
    }
}
