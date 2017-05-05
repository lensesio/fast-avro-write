[![Build Status](https://travis-ci.org/Landoop/fast-avro-write.svg?branch=master)](https://travis-ci.org/Landoop/fast-avro-write) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.landoop/fast-avro-write/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.landoop/fast-avro-write)
[![GitHub license](https://img.shields.io/github/license/Landoop/fast-avro-write.svg)]()

# fast-avro-write
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

## Blog article

http://www.landoop.com/blog/2017/05/fast-avro-write/

## Release History

0.1 - Initial release [2017-04-02]

## Performance

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

For each record count 10 runs have been made sequentially and the min and max values have been retained. All the values are in milliseconds
For Fast writes different parallelization factor has been used - see p in the header

|Record Count| Standard Min| Standard Max|Fast Min (p=8)|Fast Max (p=8)|Fast Min (p=4)|Fast Max (p=4)|Fast Min (p=6)|Fast Min (p=6)|
|------------|-------------|-------------|--------------|--------------|--------------|--------------|--------------|--------------|
|100K        |490          |530          |286           |365           |306           |562           |284           |316           |
|200K        |981          |1097         |570           |692           |545           |783           |586           |777           |
|500K        |2534         |2755         |1443          |1575          |1313          |1607          |1365          |1402          |
|1M          |5079         |5322         |2853          |2948          |2571          |2820          |2816          |2984          |
