package echoserver;

import java.nio.charset.Charset;
import java.util.HashMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class echoserverhandler extends ChannelInboundHandlerAdapter{
		private static final ChannelGroup channels= new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); 
		//private static final HashMap<String,Channel> channelmap=new HashMap<String,Channel>(); 
		 @Override
		    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		    // TODO Auto-generated method stub
			 Channel incoming = ctx.channel();
			 String sendMessage="[SERVER] - " + incoming.remoteAddress() + " has joined\n";
			 ByteBuf messageBuffer = Unpooled.buffer();
             messageBuffer.writeBytes(sendMessage.getBytes());
             
             //���� �α� ǥ��
			 System.out.println(sendMessage);
			 //�޽���(��������) ����
		        for (Channel channel : channels) {
		            channel.writeAndFlush(messageBuffer);
		        }
			 channels.add(ctx.channel());
			 channels.
		    }
		 
		 
		@Override
	    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		  String readMessage = ((ByteBuf) msg).toString(Charset.defaultCharset());
		    //3. ���ŵ� �����͸� ������ �ִ� ��Ƽ�� ����Ʈ ���� ��ü�� ���� ���ڿ� ��ü�� �о�´�.
		    Channel incoming = ctx.channel();
		    
		   //[Client: �ּ�] - [�޽��� ����]
		    String message="[Client: "+incoming.remoteAddress()+"] - [Message: "+readMessage +"]";
		    System.out.println(message);//���� �α� 
		    
		    ByteBuf messageBuffer = Unpooled.buffer();
            messageBuffer.writeBytes(message.getBytes());
            
	        for (Channel channel : channels) {
	            if (channel != incoming){
	                channel.writeAndFlush(messageBuffer);
	            }
	        }
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
	    
	    @Override
	    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	    	 Channel incoming = ctx.channel();
	         for (Channel channel : channels) {
	             channel.write("[SERVER] - " + incoming.remoteAddress() + " has left\n");
	         }
	         channels.remove(ctx.channel());
	    }
	    
	   
}
