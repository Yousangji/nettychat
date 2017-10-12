package echoclient;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

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
       // LOG.info("전송한 문자열 {}"+firstMessage.toString());
        //String sendMessage = "Hello, Netty!";
        

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String readMessage = ((ByteBuf)msg).toString(CharsetUtil.UTF_8);
        LOG.info(readMessage);
        
        JSONParser jsonParser=new JSONParser();
	    Object obj=jsonParser.parse(readMessage);
	    JSONObject jsonObj = (JSONObject) obj;
	    
	    switch((String)jsonObj.get("state")){
	  //0. 접속멤버추가
        //1. 방종료
        //2. 방나가기
        //3. 메시지전송
	    case "0": 
	    	System.out.println(jsonObj.get("nickname")+"님이 입장하셨습니다.");
	    	break;
	    case "1": 
	    	System.out.println("스트리밍이 종료되었습니다 방을 나갑니다");
	    	ctx.close();
	    	break;
	    	
	    case "2": 
	    	System.out.println(jsonObj.get("nickname")+"님이 퇴장하셨습니다.");
	    	break;
	    
	    case "3": 
	    	System.out.println(jsonObj.get("nickname") +": "+jsonObj.get("msg"));
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

}
