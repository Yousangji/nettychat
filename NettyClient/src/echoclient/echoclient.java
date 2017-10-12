package echoclient;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.json.simple.JSONObject;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

public class echoclient {
	
	  static final boolean SSL = System.getProperty("ssl") != null;
	    static final String HOST = System.getProperty("host", "192.168.1.101");
	    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
	    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));


	public static void main(String[] args) throws Exception{
		new echoclient("192.168.1.101",8007).run();
	}
	
	 private final String host;
	 private final int port;

	public echoclient(String host,int port){
		this.host=host;
		this.port=port;
		
	}
	
	public void run() throws Exception{
		// TODO Auto-generated method stub
		  //Configure SSL.git
		
      final SslContext sslContext;
      if(SSL){
          sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      }else{
          sslContext = null;
      }

      EventLoopGroup bossGroup = new NioEventLoopGroup();
      try{
          Bootstrap b = new Bootstrap();
          b.group(bossGroup)
                  .channel(NioSocketChannel.class)
                  .option(ChannelOption.TCP_NODELAY,true)
                  .handler(new ChannelInitializer<SocketChannel>() {
                      @Override
                      protected void initChannel(SocketChannel ch) throws Exception {
                          ChannelPipeline p = ch.pipeline();
                          if(sslContext != null){
                              p.addLast(sslContext.newHandler(ch.alloc(),HOST,PORT));
                          }
                          //p.addList(new LoggingHandler(LogLevel.INFO)));
                          p.addLast(new echoclienthandler());
                          p.addLast(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8));
                      }
                  });
          // Start the Client
          ChannelFuture f = b.connect(HOST,PORT).sync();
          
          //java command
          String command;
          Scanner scan=new Scanner(System.in);
          
          JSONObject jsonmsg=new JSONObject();
          
          
          System.out.println("방번호 입력: ");
          command=scan.nextLine();
          jsonmsg.put("rmnum", command);
          
          System.out.println("닉네임 입력: ");
          command=scan.nextLine();
          jsonmsg.put("nickname", command);
         
          jsonmsg.put("state", "0");
          
          BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
          while(true){
        	  
        	  if(jsonmsg.get("state").equals("0")){
        		  jsonmsg.put("msg", "");
        	  }else{
        	  String msg=in.readLine();
              jsonmsg.put("msg",msg);
        	  }
              
              String sendMessage=jsonmsg.toJSONString();
              
              System.out.println("로그 : sendmessage : "+ sendMessage);
              
        	 // String sendMessage=in.readLine() ;
              ByteBuf messageBuffer = Unpooled.buffer();
              messageBuffer.writeBytes(sendMessage.getBytes());
              f.channel().writeAndFlush(messageBuffer);
              
              jsonmsg.put("state", "3");
          }
        
      }catch(Exception e){
    	  e.printStackTrace();
      }finally {
    	//Wait until the connection is closed
          //f.channel().closeFuture().sync();
          //Shut down the event loop the terminal all threads.
          bossGroup.shutdownGracefully();
      }


	}
}
