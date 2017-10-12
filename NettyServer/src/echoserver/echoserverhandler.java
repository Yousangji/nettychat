package echoserver;

import java.nio.charset.Charset;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class echoserverhandler extends ChannelInboundHandlerAdapter{
		//private static final ChannelGroup channels= new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); 
		private static final HashMap<String,ChannelGroup> channelmap=new HashMap<String,ChannelGroup>(); 
		ChannelGroup nwgroup;
		String message=null;
		 @Override
		    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		    // TODO Auto-generated method stub
			 Channel incoming = ctx.channel();
			 //������ ���� �α� ǥ��
			 String sendMessage="[SERVER] - " + incoming.remoteAddress() + " has joined\n";
			 System.out.println(sendMessage);
		    }
		 
		 
		@Override
	    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			 // ���ŵ� �����͸� ������ �ִ� ��Ƽ�� ����Ʈ ���� ��ü�� ���� ���ڿ� ��ü�� �о�´�.
			String readMessage = ((ByteBuf) msg).toString(Charset.defaultCharset());
		    Channel incoming = ctx.channel();
		    
		   ////���� �α�  [Client: �ּ�] - [JSON �޽��� ����]
		    message="[Client: "+incoming.remoteAddress()+"] - [Message: "+readMessage +"]";
		    System.out.println(message);
		    
		    JSONParser jsonParser=new JSONParser();
		    Object obj=jsonParser.parse(readMessage);
		    JSONObject jsonObj = (JSONObject) obj;
		    
		    ByteBuf messageBuffer = Unpooled.buffer();
		   
		    
		    //1. channelgroup Ȯ��: ����� : map�� rmnum�� ���� ��� ���ο� group����
		    if(!channelmap.containsKey(jsonObj.get("rmnum"))){
				   nwgroup=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
				   channelmap.put((String)jsonObj.get("rmnum"), nwgroup);//map�� �׷��߰�
				   ////���� �α�  [Client: �ּ�] - [JSON �޽��� ����]
				    message="[Client: "+incoming.remoteAddress()+"] - [room added]";
				    System.out.println(message);
			   }
		    
		   //2. �޽������Ȯ��
		   switch((String)jsonObj.get("state")){
			
			 //2.1 ���� :���� group�� ������ �˸�, channel�߰� 
		   		case "0": 
		   		//group�� ������ ��� channel�߰�
					  channelmap.get(jsonObj.get("rmnum")).add(ctx.channel());  
					  
				////���� �α�  [Client: �ּ�] - [JSON �޽��� ����]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("rmnum")+" : Channel added]";
					  System.out.println(message);
					  
				//TODO: �����鿡�� ���� �˸�.
					
				  for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
					  //messageBuffer.writeBytes(readMessage.getBytes()); 
					  message=jsonObj.toString();
			            channel.writeAndFlush(message);
			            System.out.println(message);
			        }
				  
			  
			   break;
			   
		 
			   //2.2 ���� : ��Ʈ���� ����
			     case "1": 
			    	 
			    	////���� �α�  [Client: �ּ�] - [������]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("rmnum")+" : close room ]";
					  System.out.println(message);
					  
					   //�׷������ ���� �˸�
				   for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
					   if (channel != incoming){
						   messageBuffer.writeBytes(readMessage.getBytes());
			            channel.writeAndFlush(messageBuffer);
					   }
			        }
				   
				    //TODO: hashmap���� group ����
				   break;
				   
				   //2.3 ��������
			     case "2":
			    	 
				   if(channelmap.get("rmnum").contains(incoming)){
					   
					////���� �α�  [Client: �ּ�] - [client "num" has left]
						  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("rmnum")+" : client "+jsonObj.get("nickname")+"has left\n]";
						  System.out.println(message);
						  
					   //group���鿡�� �˸�
					   for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
						   if (channel != incoming){
							   messageBuffer.writeBytes(readMessage.getBytes());
						   channel.writeAndFlush(messageBuffer);
						   }
				         }
					   
					   channelmap.get("rmnum").remove(ctx.channel());
				   }
				   break;
				   
				 
				 /////////////1.4 message ����
			     case "3":  
				   //group���鿡�� �޽��� ������
				   for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
					   if (channel != incoming){
						   message=jsonObj.toJSONString();
			            channel.writeAndFlush(message);
					   }
			        }
				   
				////���� �α�  [Client: �ּ�] - [room "num" : client "nickname" : message]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("rmnum")+" : client "+jsonObj.get("nickname")+": Message -"+jsonObj.get("msg")+"]";
					  System.out.println(message);
				   
				   break;
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
	    	 System.out.println("[SERVER] - " + incoming.remoteAddress() + " has inactive\n");
	    	 
	    }
	    
	   
}
