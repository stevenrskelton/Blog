import org.specs2._
import org.specs2.matcher._
import com.twitter.finagle.builder.{ ServerBuilder, ClientBuilder }
import com.twitter.finagle.thrift._
import java.net.InetSocketAddress
import ca.stevenskelton.thrift.testservice._
import org.apache.thrift.protocol.TBinaryProtocol
import java.util.concurrent.TimeUnit.SECONDS
import com.twitter.util.Duration

class ExpiryCacheSpec extends mutable.SpecificationWithJUnit {

  sequential

  val socket = com.twitter.util.RandomSocket()
  val clientService = ClientBuilder().codec(ThriftClientFramedCodec()).hosts(socket).hostConnectionLimit(2).build()
  val client = new TestApi$FinagleClient(clientService)

  val filter = new BinaryProtocolToJsonLoggingFilter(TestApi, println) andThen new ExpiryCache(Duration(1, SECONDS))
  val service = ServerBuilder().codec(ThriftServerFramedCodec()).bindTo(socket).name("test")
    .build(filter andThen new TestApi$FinagleService(new TestService, new TBinaryProtocol.Factory))

  "expire items" in {
    client.w200msDelay(1).get.name == "Id1"

    var start = System.currentTimeMillis
    client.w200msDelay(1).get.name == "Id1"
    System.currentTimeMillis - start must beLessThan(175L)

    //cached
    Thread.sleep(800)
    client.w200msDelay(1).get.name == "Id1"
    System.currentTimeMillis - start must beGreaterThan(175L)
  }
}

