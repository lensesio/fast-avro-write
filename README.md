# avro-fast-write
A small library allowing you to parallelize the write to an avro file
thus achieving much better throughput


How to use it:
```scala
val datumWriter = new GenericDatumWriter[GenericRecord](schema)
val builder = FastDataFileWriterBuilder(datumWriter, out, schema)
    .withCodec(CodecFactory.snappyCodec())
    .withFlushOnEveryBlock(false)
    .withParallelization(parallelization)
    
builder.encoderFactory.configureBufferSize(4 * 1048576)
builder.encoderFactory.configureBlockSize(4 * 1048576)

val fileWriter = builder.build()
fileWriter.write(records)
```
This will write all the records to the file. If the records count passes a threshold it will parallelize the write.
You can set the threshold as well; the write method takes a default parameter threshold.
Simple!

## Release History

0.1 - first cut (2017-04-02)


## Numbers
Run on 8GB, i7-4650U, SSD
Here is the class from which the GenericRecords are created

```scala
case class StockQuote(symbol: String,
                      timestamp: Long,
                      ask: Double,
                      askSize: Int,
                      bid: Double,
                      bidSize: Int,
                      dayHigh: Double,
                      dayLow: Double,
                      lastTradeSize: Int,
                      lastTradeTime: Long,
                      open: Double,
                      previousClose: Double,
                      price: Double,
                      priceAvg50: Double,
                      priceAvg200: Double,
                      volume: Long,
                      yearHigh: Double,
                      yearLow: Double,
                      f1:String="value",
                      f2:String="value",
                      f3:String="value",
                      f4:String="value",
                      f5:String="value",
                      f6:String="value",
                      f7:String="value",
                      f8:String="value",
                      f9:String="value",
                      f10:String="value",
                      f11:String="value",
                      f12:String="value",
                      f13:String="value",
                      f14:String="value",
                      f15:String="value",
                      f16:String="value",
                      f17:String="value",
                      f18:String="value",
                      f19:String="value",
                      f20:String="value",
                      f21:String="value",
                      f22:String="value",
                      f23:String="value",
                      f24:String="value",
                      f25:String="value",
                      f26:String="value",
                      f27:String="value",
                      f28:String="value",
                      f29:String="value",
                      f30:String="value",
                      f31:String="value",
                      f32:String="value",
                      f33:String="value",
                      f34:String="value",
                      f35:String="value",
                      f36:String="value",
                      f37:String="value",
                      f38:String="value",
                      f39:String="value",
                      f40:String="value",
                      f41:String="value",
                      f42:String="value",
                      f43:String="value",
                      f44:String="value",
                      f45:String="value",
                      f46:String="value",
                      f47:String="value",
                      f48:String="value",
                      f49:String="value",
                      f50:String="value",
                      f51:String="value",
                      f52:String="value",
                      f53:String="value",
                      f54:String="value",
                      f55:String="value",
                      f56:String="value",
                      f57:String="value",
                      f58:String="value",
                      f59:String="value",
                      f60:String="value"
                     )
```
### Standard 
1. Writing 100k records
```
Run number 0 took 519 ms
Run number 1 took 496 ms
Run number 2 took 530 ms
Run number 3 took 505 ms
Run number 4 took 509 ms
Run number 5 took 493 ms
Run number 6 took 490 ms
Run number 7 took 491 ms
Run number 8 took 505 ms
Run number 9 took 502 ms
Min run took 490 ms
Max run took 530 ms
Avg run took 504 ms
```
2. Writing 200k records
```
Run number 0 took 1097 ms
Run number 1 took 1067 ms
Run number 2 took 981 ms
Run number 3 took 1011 ms
Run number 4 took 983 ms
Run number 5 took 1007 ms
Run number 6 took 1001 ms
Run number 7 took 1022 ms
Run number 8 took 990 ms
Run number 9 took 1069 ms
Min run took 981 ms
Max run took 1097 ms
Avg run took 1022 ms

```

3. Writing 500k records

```
Run number 0 took 2755 ms
Run number 1 took 2735 ms
Run number 2 took 2556 ms
Run number 3 took 2562 ms
Run number 4 took 2558 ms
Run number 5 took 2534 ms
Run number 6 took 2564 ms
Run number 7 took 2563 ms
Run number 8 took 2570 ms
Run number 9 took 2585 ms
Min run took 2534 ms
Max run took 2755 ms
Avg run took 2598 ms
```

4.Writing 1M records

```
Run number 0 took 5079 ms
Run number 1 took 5117 ms
Run number 2 took 5312 ms
Run number 3 took 5285 ms
Run number 4 took 5306 ms
Run number 5 took 5354 ms
Run number 6 took 5393 ms
Run number 7 took 5485 ms
Run number 8 took 5477 ms
Run number 9 took 5412 ms
Min run took 5079 ms
Max run took 5485 ms
Avg run took 5322 ms
```

### Fast write 
1. Fast write 100K

```
Run number 0 took 321 ms
Run number 1 took 365 ms
Run number 2 took 295 ms
Run number 3 took 300 ms
Run number 4 took 299 ms
Run number 5 took 296 ms
Run number 6 took 287 ms
Run number 7 took 286 ms
Run number 8 took 287 ms
Run number 9 took 293 ms
Min run took 286 ms
Max run took 365 ms
Avg run took 302 ms
```

2. Writing 200k records

```
Run number 0 took 692 ms
Run number 1 took 643 ms
Run number 2 took 589 ms
Run number 3 took 600 ms
Run number 4 took 574 ms
Run number 5 took 571 ms
Run number 6 took 570 ms
Run number 7 took 592 ms
Run number 8 took 575 ms
Run number 9 took 585 ms
Min run took 570 ms
Max run took 692 ms
Avg run took 599 ms
```

3. Writing 500k records
```
Run number 0 took 1575 ms
Run number 1 took 1464 ms
Run number 2 took 1443 ms
Run number 3 took 1444 ms
Run number 4 took 1462 ms
Run number 5 took 1457 ms
Run number 6 took 1469 ms
Run number 7 took 1504 ms
Run number 8 took 1489 ms
Run number 9 took 1507 ms
Min run took 1443 ms
Max run took 1575 ms
Avg run took 1481 ms
```

4 Writing 1M records

```
Run number 0 took 2948 ms
Run number 1 took 2911 ms
Run number 2 took 2893 ms
Run number 3 took 2924 ms
Run number 4 took 2925 ms
Run number 5 took 2853 ms
Run number 6 took 2872 ms
Run number 7 took 2888 ms
Run number 8 took 2908 ms
Run number 9 took 2914 ms
Min run took 2853 ms
Max run took 2948 ms
Avg run took 2903 ms
```