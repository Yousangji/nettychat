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
			 //서버에 접속 로그 표시
			 String sendMessage="[SERVER] - " + incoming.remoteAddress() + " has joined\n";
			 System.out.println(sendMessage);
		    }
		 
		 
		@Override
	    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			
			// 수신된 데이터를 가지고 있는 네티의 바이트 버퍼 객체로 부터 문자열 객체를 읽어온다.
			String readMessage = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
		    Channel incoming = ctx.channel();
		    
		    if(readMessage.equals("pong")){
		    	////////////////////////////////////temp
		    	System.out.println("getpong");
		    }else{
		   ////서버 로그  [Client: 주소] - [JSON 메시지 내용]
		    message="[Client: "+incoming.remoteAddress()+"] - [Message: "+readMessage +"]";
		    System.out.println(message);
		    
		    JSONParser jsonParser=new JSONParser();
		    Object obj=jsonParser.parse(readMessage);
		    JSONObject jsonObj = (JSONObject) obj;
		    
		    ByteBuf messageBuffer = Unpooled.buffer();
		   
		  	
		    
		    //1. channelgroup 확인: 방생성 : map에 rmnum가 없을 경우 새로운 group생성
		    if(!channelmap.containsKey(jsonObj.get("msg_rmnum"))){
				   nwgroup=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
				   channelmap.put((String)jsonObj.get("msg_rmnum"), nwgroup);//map에 그룹추가
				   ////서버 로그  [Client: 주소] - [JSON 메시지 내용]
				    message="[Client: "+incoming.remoteAddress()+"] - [roomnum"+jsonObj.get("msg_rmnum")+ "added]";
				    System.out.println(message);
			   }
		    
		   //2. 메시지기능확인
		   switch((String)jsonObj.get("msg_state")){
			
			 //2.1 접속 :기존 group에 접속을 알림, channel추가 
		   		case "0": 
		   		//group에 접속한 멤버 channel추가
					  channelmap.get(jsonObj.get("msg_rmnum")).add(ctx.channel());
					  channelidUseridMap.put(incoming.id(),jsonObj.get("msg_userid").toString());
					  useridRoomidMap.put(jsonObj.get("msg_userid").toString(), jsonObj.get("msg_rmnum").toString());
					  
					  
				////서버 로그  [Client: 주소] - [JSON 메시지 내용]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : Channel added]";
					  System.out.println(message);
					  
				//유저들에게 접속 알림.
				  for (Channel channel : channelmap.get(jsonObj.get("msg_rmnum"))) {
					  //messageBuffer.writeBytes(readMessage.getBytes()); 
					  if(!ctx.channel().equals(channel)){
					  message=jsonObj.toString();
			            channel.writeAndFlush(message);
			            System.out.println(message);
			            //TODO: 접속 channel에 접속 확인 메시지
					  }else{
					
					  }
			        }
				  
			  
			   break;
			   
		 
			   //2.2 종료 : 스트리밍 종료
			     case "1": 
			    	 
			    	////서버 로그  [Client: 주소] - [방종료]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : close room ]";
					  System.out.println(message);
					  
					   //그룹원에게 종료 알림
				   for (Channel channel : channelmap.get(jsonObj.get("msg_rmnum"))) {
					   if (channel != incoming){
						   messageBuffer.writeBytes(readMessage.getBytes());
			            channel.writeAndFlush(messageBuffer);
			            channelidUseridMap.remove(incoming.id());
						useridRoomidMap.remove(jsonObj.get("msg_userid").toString());
						  
					   }
			        }
				   
				    //TODO: hashmap에서 group 삭제
				   channelmap.remove(jsonObj.get("msg_rmnum"));
				   
				   break;
				   
				   //2.3 접속종료
			     case "2":
			    	 
				   if(channelmap.get("msg_rmnum").contains(incoming)){
					   
					////서버 로그  [Client: 주소] - [client "num" has left]
						  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : client "+jsonObj.get("msg_nickname")+"has left\n]";
						  System.out.println(message);
						  
					   //group원들에게 알림
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
				   
				 
				 /////////////1.4 message 전송
			     case "3":  
				   //group원들에게 메시지 보내기
				   for (Channel channel : channelmap.get(jsonObj.get("msg_rmnum"))) {
					   if (channel != incoming){
						   message=jsonObj.toJSONString();
			            channel.writeAndFlush(message);
					   }
			        }
				   
				////서버 로그  [Client: 주소] - [room "num" : client "nickname" : message]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("msg_rmnum")+" : client "+jsonObj.get("nickname")+": Message -"+jsonObj.get("msg")+"]";
					  System.out.println(message);
				   
				   break;
				   
				   //재접속
			     case "4": 
				   		//group에 접속한 멤버 channel추가
							  channelmap.get(jsonObj.get("msg_rmnum")).add(ctx.channel());
							  channelidUseridMap.put(incoming.id(),jsonObj.get("msg_userid").toString());
							  useridRoomidMap.put(jsonObj.get("msg_userid").toString(), jsonObj.get("msg_rmnum").toString());
							  
							  
						////서버 로그  [Client: 주소] - [JSON 메시지 내용]
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
