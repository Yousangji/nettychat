package echoclient;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class echoclienthandler extends ChannelInboundHandlerAdapter {
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(echoclienthandler.class);
    private final ByteBuf firstMessage;

    public echoclienthandler(){
        firstMessage  = Unpooled.buffer(echoclient.SIZE);
    	
    	
        for(int i =0 ; i< firstMessage.capacity();i++){
            firstMessage.writeByte((byte)i);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("전송한 문자열 {}"+firstMessage.toString());
        //String sendMessage = "Hello, Netty!";
        

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String readMessage = ((ByteBuf)msg).toString(Charset.defaultCharset());
        LOG.info(readMessage);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
