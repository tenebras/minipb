# minipb

Minifier for ProtocolBuffer files

Allows reducing proto files to only necessary services, service methods and types. 

Example:
```protobuf
syntax = "proto3";

package com.example;

service Test {
  rpc Foo (Foo.Request) returns (Foo.Response);
  rpc Bar (BarRequest) returns (BarResponse);
  rpc Baz (BazResponse) returns (BazResponse);
}

message Foo {
  string ignoredField = 1;
  ...
  
  message Request {
    string test = 1;
  }
  
  message Response {
    int32 test = 1;
  }
  
  enum Error {
    UNKNOWN = 0;
    ILLEGAL_ARGUMENT = 1;
    ...
  }
}

message BarRequest {...}
message BarResponse {...}
message BazRequest {...}
message BazResponse {...}
```

Resulting file after applying `java -jar minipb.jar -m Test.Foo`:

```protobuf
syntax = "proto3";

package com.example;

service Test {
  rpc Foo (Foo.Request) returns (Foo.Response);
}

message Foo {
  message Request {
    string test = 1;
  }
  
  message Response {
    int32 test = 1;
  }  
}
```
Important note: Message `Foo` was also included to keep namespace for `Request` and `Response`. 
But not referenced content (fields, options, other subtypes) would be completely omitted. It's still possible to force it's content or any other type with option `--type`/`-t`. 


### Arguments and options
```
Arguments: 
    input -> Input file { String }
    outputFolder -> Output folder { String }
Options: 
    --method, -m -> Service method. May be method name or Service.Method { String }
    --exclude-method, -em -> Service method to exclude. May be method name or Service.Method { String }
    --service, -s -> Service to include { String }
    --exclude-service, -es -> Service to exclude { String }
    --clear-output, -co [false] -> Clear output folder 
    --type, -t -> Type name to force include. Should be FQN including package name { String }
    --ignore-services, -is [false] -> Ignore all services would be included only forced types 
    --help, -h -> Usage info 
```

### Examples
- Reduce proto file to single method `Bar` of service `Foo` and all it's dependencies:

`java -jar ./minipb-0.1.jar ./input-file.proto ./output -m Foo.Bar`

- Extract method `Bar` from any declared service:

`java -jar ./minipb-0.1.jar ./input-file.proto ./output -m Bar`

- Extract one service with name `Foo`:

```java -jar ./minipb-0.1.jar ./input-file.proto ./output -s Foo```

- Extract everything except method `Baz` declared in any service:

`java -jar ./minipb-0.1.jar ./input-file.proto ./output -em Baz`

- Extract single type with dependencies:

`java -jar ./minipb-0.1.jar ./input-file.proto ./output -is -t com.example.Response.Error`
