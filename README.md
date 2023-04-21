# minipb

Minifier for ProtocolBuffer files

Allows reducing proto files to only necessary services, service methods and types. 

### Arguments and options
```
  Arguments:
  input -> Input file { String }
  outputFolder -> Output folder { String }
  Options:
  --method, -m -> Service method. May be method name or Service.Method { String }
  --excludeMethod, -em -> Service method to exclude. May be method name or Service.Method { String }
  --service, -s -> Service to include { String }
  --excludeService, -es -> Service to exclude { String }
  --clearOutput, -co [false] -> Clear output folder
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