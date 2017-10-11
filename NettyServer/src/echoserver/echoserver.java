package echoserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;

public class echoserver {

	static final boolean SSL =  System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port","8007"));

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		  //Configure SSL
        final SslContext sslContext;
        if(SSL){
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        }else{
            sslContext = null;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if(sslContext != null){
                                p.addLast(sslContext.newHandler(ch.alloc()));
                            }
                            p.addLast(new echoserverhandler());
                            p.addLast(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            // Start the Server
            ChannelFuture f = b.bind(PORT).sync();

            //Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        }finally {
            //Shut down all event loops to terminate all threads.
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }


	}

}
