use indexmap::{IndexMap, IndexSet};
use nanoserde::{DeJson, SerJson};
use rustc_hash::FxBuildHasher;

pub type FastIndexMap<K, V> = IndexMap<K, V, FxBuildHasher>;
pub type FastIndexSet<T> = IndexSet<T, FxBuildHasher>;

#[derive(Debug, Clone, Default)]
pub struct DeterministicMap<K, V>(pub FastIndexMap<K, V>);

impl<K, V> DeterministicMap<K, V> {
    pub fn new() -> Self {
        Self(FastIndexMap::default())
    }
}

impl<K: std::hash::Hash + Eq, V> std::iter::FromIterator<(K, V)> for DeterministicMap<K, V> {
    fn from_iter<I: IntoIterator<Item = (K, V)>>(iter: I) -> Self {
        let mut map = FastIndexMap::default();
        map.extend(iter);
        DeterministicMap(map)
    }
}

impl<K: std::hash::Hash + Eq, V> Extend<(K, V)> for DeterministicMap<K, V> {
    fn extend<I: IntoIterator<Item = (K, V)>>(&mut self, iter: I) {
        self.0.extend(iter);
    }
}

impl<K: std::hash::Hash + Eq, V: PartialEq> PartialEq for DeterministicMap<K, V> {
    fn eq(&self, other: &Self) -> bool {
        self.0 == other.0
    }
}

impl<K, V> std::ops::Deref for DeterministicMap<K, V> {
    type Target = FastIndexMap<K, V>;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl<K, V> std::ops::DerefMut for DeterministicMap<K, V> {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.0
    }
}

impl<V: DeJson> DeJson for DeterministicMap<String, V> {
    fn de_json(
        state: &mut nanoserde::DeJsonState,
        input: &mut std::str::Chars,
    ) -> Result<Self, nanoserde::DeJsonErr> {
        state.next_tok(input)?;
        let mut obj = FastIndexMap::default();
        while state.tok != nanoserde::DeJsonTok::CurlyClose {
            if state.tok == nanoserde::DeJsonTok::Str {
                let key = state.strbuf.clone();
                state.next_tok(input)?;
                if state.tok != nanoserde::DeJsonTok::Colon {
                    return Err(state.err_exp("Colon"));
                }
                state.next_tok(input)?;
                obj.insert(key, DeJson::de_json(state, input)?);
                if state.tok == nanoserde::DeJsonTok::Comma {
                    state.next_tok(input)?;
                }
            } else {
                return Err(state.err_exp("String key"));
            }
        }
        state.next_tok(input)?;
        Ok(DeterministicMap(obj))
    }
}

impl<V: SerJson> SerJson for DeterministicMap<String, V> {
    fn ser_json(&self, d: usize, s: &mut nanoserde::SerJsonState) {
        s.out.push('{');
        for (i, (k, v)) in self.0.iter().enumerate() {
            if i > 0 {
                s.out.push(',');
            }
            k.ser_json(d, s);
            s.out.push(':');
            v.ser_json(d, s);
        }
        s.out.push('}');
    }
}

#[derive(Debug, Clone, Default)]
pub struct DeterministicSet<T>(pub FastIndexSet<T>);

impl<T: std::hash::Hash + Eq> std::iter::FromIterator<T> for DeterministicSet<T> {
    fn from_iter<I: IntoIterator<Item = T>>(iter: I) -> Self {
        let mut set = FastIndexSet::default();
        set.extend(iter);
        DeterministicSet(set)
    }
}

impl<T: std::hash::Hash + Eq> Extend<T> for DeterministicSet<T> {
    fn extend<I: IntoIterator<Item = T>>(&mut self, iter: I) {
        self.0.extend(iter);
    }
}

impl<T: std::hash::Hash + Eq> PartialEq for DeterministicSet<T> {
    fn eq(&self, other: &Self) -> bool {
        self.0 == other.0
    }
}

impl<T> std::ops::Deref for DeterministicSet<T> {
    type Target = FastIndexSet<T>;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl<T> std::ops::DerefMut for DeterministicSet<T> {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.0
    }
}

impl<T: DeJson + std::hash::Hash + Eq> DeJson for DeterministicSet<T> {
    fn de_json(
        state: &mut nanoserde::DeJsonState,
        input: &mut std::str::Chars,
    ) -> Result<Self, nanoserde::DeJsonErr> {
        state.next_tok(input)?;
        let mut set = FastIndexSet::default();
        while state.tok != nanoserde::DeJsonTok::BlockClose {
            set.insert(DeJson::de_json(state, input)?);
            if state.tok == nanoserde::DeJsonTok::Comma {
                state.next_tok(input)?;
            }
        }
        state.next_tok(input)?;
        Ok(DeterministicSet(set))
    }
}

impl<T: SerJson> SerJson for DeterministicSet<T> {
    fn ser_json(&self, d: usize, s: &mut nanoserde::SerJsonState) {
        s.out.push('[');
        for (i, item) in self.0.iter().enumerate() {
            if i > 0 {
                s.out.push(',');
            }
            item.ser_json(d, s);
        }
        s.out.push(']');
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum JsonValue {
    String(String),
    Integer(i64),
    Float(f64),
    Bool(bool),
    Null,
    Array(Vec<JsonValue>),
    Object(DeterministicMap<String, JsonValue>),
}

impl Default for JsonValue {
    fn default() -> Self {
        JsonValue::Null
    }
}

impl DeJson for JsonValue {
    fn de_json(
        state: &mut nanoserde::DeJsonState,
        input: &mut std::str::Chars,
    ) -> Result<Self, nanoserde::DeJsonErr> {
        match state.tok {
            nanoserde::DeJsonTok::Str => {
                let s = state.strbuf.clone();
                state.next_tok(input)?;
                Ok(JsonValue::String(s))
            }
            nanoserde::DeJsonTok::F64(n) => {
                state.next_tok(input)?;
                Ok(JsonValue::Float(n))
            }
            nanoserde::DeJsonTok::U64(n) => {
                state.next_tok(input)?;
                Ok(JsonValue::Integer(n as i64))
            }
            nanoserde::DeJsonTok::I64(n) => {
                state.next_tok(input)?;
                Ok(JsonValue::Integer(n))
            }
            nanoserde::DeJsonTok::Bool(b) => {
                state.next_tok(input)?;
                Ok(JsonValue::Bool(b))
            }
            nanoserde::DeJsonTok::Null => {
                state.next_tok(input)?;
                Ok(JsonValue::Null)
            }
            nanoserde::DeJsonTok::BlockOpen => {
                state.next_tok(input)?;
                let mut arr = Vec::new();
                while state.tok != nanoserde::DeJsonTok::BlockClose {
                    arr.push(JsonValue::de_json(state, input)?);
                    if state.tok == nanoserde::DeJsonTok::Comma {
                        state.next_tok(input)?;
                    }
                }
                state.next_tok(input)?;
                Ok(JsonValue::Array(arr))
            }
            nanoserde::DeJsonTok::CurlyOpen => {
                Ok(JsonValue::Object(DeterministicMap::de_json(state, input)?))
            }
            _ => Err(state.err_exp("JsonValue")),
        }
    }
}

impl SerJson for JsonValue {
    fn ser_json(&self, d: usize, s: &mut nanoserde::SerJsonState) {
        match self {
            JsonValue::String(v) => v.ser_json(d, s),
            JsonValue::Integer(v) => v.ser_json(d, s),
            JsonValue::Float(v) => v.ser_json(d, s),
            JsonValue::Bool(v) => v.ser_json(d, s),
            JsonValue::Null => s.out.push_str("null"),
            JsonValue::Array(v) => v.ser_json(d, s),
            JsonValue::Object(v) => v.ser_json(d, s),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_deterministic_map_order() {
        let mut map = DeterministicMap::new();
        map.insert("b".to_string(), JsonValue::Integer(2));
        map.insert("a".to_string(), JsonValue::Integer(1));
        map.insert("c".to_string(), JsonValue::Integer(3));

        let keys: Vec<&String> = map.keys().collect();
        assert_eq!(keys, vec!["b", "a", "c"]);

        let json = SerJson::serialize_json(&map);
        assert_eq!(json, r#"{"b":2,"a":1,"c":3}"#);
    }

    #[test]
    fn test_deterministic_set_order() {
        let mut set = DeterministicSet::default();
        set.insert("b".to_string());
        set.insert("a".to_string());
        set.insert("c".to_string());

        let items: Vec<&String> = set.iter().collect();
        assert_eq!(items, vec!["b", "a", "c"]);

        let json = SerJson::serialize_json(&set);
        assert_eq!(json, r#"["b","a","c"]"#);
    }

    #[test]
    fn test_json_value_deserialize() {
        let json = r#"{"str":"hello","int":42,"float":3.14,"bool":true,"null":null,"arr":[1,2],"obj":{"key":"val"}}"#;
        let val: JsonValue = DeJson::deserialize_json(json).unwrap();

        match val {
            JsonValue::Object(map) => {
                assert_eq!(
                    map.get("str"),
                    Some(&JsonValue::String("hello".to_string()))
                );
                assert_eq!(map.get("int"), Some(&JsonValue::Integer(42)));
                match map.get("float") {
                    Some(JsonValue::Float(f)) => assert!((f - 3.14).abs() < 0.0001),
                    _ => panic!("Expected float"),
                }
                assert_eq!(map.get("bool"), Some(&JsonValue::Bool(true)));
                assert_eq!(map.get("null"), Some(&JsonValue::Null));

                match map.get("arr") {
                    Some(JsonValue::Array(arr)) => {
                        assert_eq!(arr.len(), 2);
                        assert_eq!(arr[0], JsonValue::Integer(1));
                        assert_eq!(arr[1], JsonValue::Integer(2));
                    }
                    _ => panic!("Expected array"),
                }

                match map.get("obj") {
                    Some(JsonValue::Object(inner_map)) => {
                        assert_eq!(
                            inner_map.get("key"),
                            Some(&JsonValue::String("val".to_string()))
                        );
                    }
                    _ => panic!("Expected object"),
                }
            }
            _ => panic!("Expected object"),
        }
    }

    #[test]
    fn test_json_value_serialize() {
        let mut map = DeterministicMap::new();
        map.insert("a".to_string(), JsonValue::String("hello".to_string()));
        map.insert(
            "b".to_string(),
            JsonValue::Array(vec![JsonValue::Integer(1), JsonValue::Bool(false)]),
        );

        let val = JsonValue::Object(map);
        let json = SerJson::serialize_json(&val);
        assert_eq!(json, r#"{"a":"hello","b":[1,false]}"#);
    }
}
