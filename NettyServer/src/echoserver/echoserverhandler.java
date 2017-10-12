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
			 //서버에 접속 로그 표시
			 String sendMessage="[SERVER] - " + incoming.remoteAddress() + " has joined\n";
			 System.out.println(sendMessage);
		    }
		 
		 
		@Override
	    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			 // 수신된 데이터를 가지고 있는 네티의 바이트 버퍼 객체로 부터 문자열 객체를 읽어온다.
			String readMessage = ((ByteBuf) msg).toString(Charset.defaultCharset());
		    Channel incoming = ctx.channel();
		    
		   ////서버 로그  [Client: 주소] - [JSON 메시지 내용]
		    message="[Client: "+incoming.remoteAddress()+"] - [Message: "+readMessage +"]";
		    System.out.println(message);
		    
		    JSONParser jsonParser=new JSONParser();
		    Object obj=jsonParser.parse(readMessage);
		    JSONObject jsonObj = (JSONObject) obj;
		    
		    ByteBuf messageBuffer = Unpooled.buffer();
		   
		    
		    //1. channelgroup 확인: 방생성 : map에 rmnum가 없을 경우 새로운 group생성
		    if(!channelmap.containsKey(jsonObj.get("rmnum"))){
				   nwgroup=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
				   channelmap.put((String)jsonObj.get("rmnum"), nwgroup);//map에 그룹추가
				   ////서버 로그  [Client: 주소] - [JSON 메시지 내용]
				    message="[Client: "+incoming.remoteAddress()+"] - [room added]";
				    System.out.println(message);
			   }
		    
		   //2. 메시지기능확인
		   switch((String)jsonObj.get("state")){
			
			 //2.1 접속 :기존 group에 접속을 알림, channel추가 
		   		case "0": 
		   		//group에 접속한 멤버 channel추가
					  channelmap.get(jsonObj.get("rmnum")).add(ctx.channel());  
					  
				////서버 로그  [Client: 주소] - [JSON 메시지 내용]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("rmnum")+" : Channel added]";
					  System.out.println(message);
					  
				//TODO: 유저들에게 접속 알림.
					
				  for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
					  //messageBuffer.writeBytes(readMessage.getBytes()); 
					  message=jsonObj.toString();
			            channel.writeAndFlush(message);
			            System.out.println(message);
			        }
				  
			  
			   break;
			   
		 
			   //2.2 종료 : 스트리밍 종료
			     case "1": 
			    	 
			    	////서버 로그  [Client: 주소] - [방종료]
					  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("rmnum")+" : close room ]";
					  System.out.println(message);
					  
					   //그룹원에게 종료 알림
				   for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
					   if (channel != incoming){
						   messageBuffer.writeBytes(readMessage.getBytes());
			            channel.writeAndFlush(messageBuffer);
					   }
			        }
				   
				    //TODO: hashmap에서 group 삭제
				   break;
				   
				   //2.3 접속종료
			     case "2":
			    	 
				   if(channelmap.get("rmnum").contains(incoming)){
					   
					////서버 로그  [Client: 주소] - [client "num" has left]
						  message="[Client: "+incoming.remoteAddress()+"] - [room "+jsonObj.get("rmnum")+" : client "+jsonObj.get("nickname")+"has left\n]";
						  System.out.println(message);
						  
					   //group원들에게 알림
					   for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
						   if (channel != incoming){
							   messageBuffer.writeBytes(readMessage.getBytes());
						   channel.writeAndFlush(messageBuffer);
						   }
				         }
					   
					   channelmap.get("rmnum").remove(ctx.channel());
				   }
				   break;
				   
				 
				 /////////////1.4 message 전송
			     case "3":  
				   //group원들에게 메시지 보내기
				   for (Channel channel : channelmap.get(jsonObj.get("rmnum"))) {
					   if (channel != incoming){
						   message=jsonObj.toJSONString();
			            channel.writeAndFlush(message);
					   }
			        }
				   
				////서버 로그  [Client: 주소] - [room "num" : client "nickname" : message]
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
