package echoserver;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

public class echoserverhandler extends ChannelInboundHandlerAdapter{
		//private static final ChannelGroup channels= new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); 
		private static final HashMap<String,ChannelGroup> channelmap=new HashMap<String,ChannelGroup>(); 
		ChannelGroup nwgroup;
		String message=null;
		private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled
				.unreleasableBuffer(Unpooled.copiedBuffer("Heartbeat",
						CharsetUtil.UTF_8));  
		  private long lastping = 0;
		  
		  
		  //Temporary
		  Map<ChannelId,String> channelidUseridMap=new ConcurrentHashMap<>();
		  Map<String,String> useridRoomidMap=new ConcurrentHashMap<>();
		  
		  
		  
		  
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
			String readMessage = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
		    Channel incoming = ctx.channel();
		    
		    if(readMessage.equals("pong")){
		    	////////////////////////////////////temp
		    	System.out.println("getpong");
		    }else{
		   ////���� �α�  [Client: �ּ�] - [JSON �޽��� ����]
		    message="[Client: "+incoming.remoteAddress()+"] - [Message: "+readMessage +"]";
		    System.out.println(message);
		    
		    JSONParser jsonParser=new JSONParser();
		    Object obj=jsonParser.parse(readMessage);
		    JSONObject jsonObj = (JSONObject) obj;
		    
		    ByteBuf messageBuffer = Unpooled.buffer();
		   
		  	
		    
		    //1. channelgroup Ȯ��: ����� : map�� rmnum�� ���� ��� ���ο� group����
		    if(!channelmap.containsKey(jsonObj.get("msg_rmnum"))){
				   nwgroup=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
				   channelmap.put((String)jsonObj.get("msg_rmnum"), nwgroup);//map�� �׷��߰�
				   ////���� �α�  [Client: �ּ�] - [JSON �޽��� ����]
				    message="[Client: "+incoming.remoteAddress()+"] - [roomnum"+jsonObj.get("msg_rmnum")+ "added]";
				    System.out.println(message);
			   }
		    
		   //2. �޽������Ȯ��
		   switch((String)jsonObj.get("msg_state")){
			
			 //2.1 ���� :���� group�� ������ �˸�, channel�߰� 
		   		case "0": 
		   		//group�� ������ ��� channel�߰�
					  channelmap.get(jsonObj.get("msg_rmnum")).add(ctx.channel());
					  channelidUseridMap.put(incoming.id(),jsonObj.get("msg_userid").toString());
					  useridRoomidMap.put(jsonObj.get("msg_userid").toString(), jsonObj.get("msg_rmnum").toString());
					  
					  
				////���� �α�  [Client: �ּ�] - [JSON �޽��� ����]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : Channel added]";
					  System.out.println(message);
					  
				//�����鿡�� ���� �˸�.
				  for (Channel channel : channelmap.get(jsonObj.get("msg_rmnum"))) {
					  //messageBuffer.writeBytes(readMessage.getBytes()); 
					  if(!ctx.channel().equals(channel)){
					  message=jsonObj.toString();
			            channel.writeAndFlush(message);
			            System.out.println(message);
			            //TODO: ���� channel�� ���� Ȯ�� �޽���
					  }else{
					
					  }
			        }
				  
			  
			   break;
			   
		 
			   //2.2 ���� : ��Ʈ���� ����
			     case "1": 
			    	 
			    	////���� �α�  [Client: �ּ�] - [������]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : close room ]";
					  System.out.println(message);
					  
					   //�׷������ ���� �˸�
				   for (Channel channel : channelmap.get(jsonObj.get("msg_rmnum"))) {
					   if (channel != incoming){
						   messageBuffer.writeBytes(readMessage.getBytes());
			            channel.writeAndFlush(messageBuffer);
			            channelidUseridMap.remove(incoming.id());
						useridRoomidMap.remove(jsonObj.get("msg_userid").toString());
						  
					   }
			        }
				   
				    //TODO: hashmap���� group ����
				   channelmap.remove(jsonObj.get("msg_rmnum"));
				   
				   break;
				   
				   //2.3 ��������
			     case "2":
			    	 
				   if(channelmap.get("msg_rmnum").contains(incoming)){
					   
					////���� �α�  [Client: �ּ�] - [client "num" has left]
						  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : client "+jsonObj.get("msg_nickname")+"has left\n]";
						  System.out.println(message);
						  
					   //group���鿡�� �˸�
					   for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
						   if (channel != incoming){
							   messageBuffer.writeBytes(readMessage.getBytes());
						   channel.writeAndFlush(messageBuffer);
						   }
				         }
					   
					   channelmap.get("msg_rmnum").remove(ctx.channel());
					   channelidUseridMap.remove(incoming.id());
					   useridRoomidMap.remove(jsonObj.get("msg_userid").toString());
						  
						  
				   }
				   break;
				   
				 
				 /////////////1.4 message ����
			     case "3":  
				   //group���鿡�� �޽��� ������
				   for (Channel channel : channelmap.get(jsonObj.get("msg_rmnum"))) {
					   if (channel != incoming){
						   message=jsonObj.toJSONString();
			            channel.writeAndFlush(message);
					   }
			        }
				   
				////���� �α�  [Client: �ּ�] - [room "num" : client "nickname" : message]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : client "+jsonObj.get("nickname")+": Message -"+jsonObj.get("msg")+"]";
					  System.out.println(message);
				   
				   break;
				   
				   //������
			     case "4": 
				   		//group�� ������ ��� channel�߰�
							  channelmap.get(jsonObj.get("msg_rmnum")).add(ctx.channel());
							  channelidUseridMap.put(incoming.id(),jsonObj.get("msg_userid").toString());
							  useridRoomidMap.put(jsonObj.get("msg_userid").toString(), jsonObj.get("msg_rmnum").toString());
							  
							  
						////���� �α�  [Client: �ּ�] - [JSON �޽��� ����]
							  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : Channel added]";
							  System.out.println(message);
							  
					  
					   break;
					   
		   }
		   
		   //return message result
		   jsonObj.put("status", "-1");
		   jsonObj.put("data", "OK");
		   incoming.writeAndFlush(jsonObj.toJSONString());
		   
		    }
		    
	    }

	    @Override
	    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	        ctx.flush();
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	        cause.printStackTrace();
	        String userid= channelidUseridMap.get(ctx.channel().id());
	        channelidUseridMap.remove(ctx.channel().id());
			useridRoomidMap.remove(userid);
			 channelmap.get("msg_rmnum").remove(ctx.channel());
	        ctx.close();
	        
	      
			
	    }
	    
	    @Override
	    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	    	 Channel incoming = ctx.channel();
	    	 System.out.println("[SERVER] - " + incoming.remoteAddress() + " has inactive\n");
	    	 
	    }
	    
	  
	    @Override
	    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
	        if (evt instanceof IdleStateEvent) {
	            IdleStateEvent event = (IdleStateEvent) evt;
	            if (event.state().equals(IdleState.READER_IDLE)) {
	                System.out.println("READER_IDLE");
	                if (lastping != 0L) {
	                    long time = (System.currentTimeMillis() - lastping) / 1000;
	                    System.out.println("Time : " + time);
	                    if (time > 3) {
	                        System.err.println("No heart beat received in 3 seconds, close channel.");
	                        String userid= channelidUseridMap.get(ctx.channel().id());
	            	        channelidUseridMap.remove(ctx.channel().id());
	            	        String rmnum=useridRoomidMap.get(userid);
	            	        useridRoomidMap.remove(userid);
	            			channelmap.get(rmnum).remove(ctx.channel());
	                        ctx.close();
	                    }
	                }
	            } else if (event.state().equals(IdleState.WRITER_IDLE)) {
	                System.out.println("WRITER_IDLE");
	            } else if (event.state().equals(IdleState.ALL_IDLE)) {
	                System.out.println("ALL_IDLE");
	                if (lastping == 0L) {
	                    lastping = System.currentTimeMillis();
	                }
	              
	    		
	                ctx.channel().writeAndFlush("ping");
	            }
	        }
	        super.userEventTriggered(ctx, evt); //To change body of generated methods, choose Tools | Templates.
	    }
	   
	    	   
}
