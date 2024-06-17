package com.ceit.workstation.grpc;

import ZjAdmin.Account.*;
import ZjAdmin.File.DownloadFileReply;
import ZjAdmin.File.DownloadFileRequest;
import ZjAdmin.WebAdmin.*;
import com.ceit.bootstrap.ConfigLoader;
import com.ceit.response.Result;
import io.grpc.ConnectivityState;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ZjWebAdminClient {

    private static final Logger logger = Logger.getLogger(ZjWebAdminClient.class.getName());

    private static ManagedChannel channel;
    private static WebAdminServiceGrpc.WebAdminServiceBlockingStub blockingStub;

    //参数配置
    private static String host;
    private static int port;
    private static boolean userSSL = false;
    private static String username= "";
    private static String password = "";

    public ZjWebAdminClient(){
        String ip = ConfigLoader.getConfig("web.grpc.ip");
        int port = Integer.parseInt(ConfigLoader.getConfig("web.grpc.port"));
        String token = ConfigLoader.getConfig("web.grpc.token");
        setConfig(ip,port,false,token);
    }
    public static void setConfig(String _host, int _port, boolean _userSSL, String _token)
    {
        host= _host;
        port= _port;

        //暂时不支持ssl
        userSSL=false;

        if(_token!=null) {
            username= _token;
            password= _token;
        }
    }

    private static Result connectGrpcServer() {

        try {
            //如果已经连接，重用连接
            if(channel!=null && !channel.getState(true).equals(ConnectivityState.READY)) {
                return new Result(200, "ok");
            }

            String url = host +":" + port;

            //如果使用SSL
            channel = Grpc.newChannelBuilder(url, InsecureChannelCredentials.create()).build();
            blockingStub = WebAdminServiceGrpc.newBlockingStub(channel);

            //认证一次，如果服务器没有控制直接成功
            LoginRequest request = LoginRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .build();

            LoginReply reply = blockingStub.login(request);
            if(reply.getCode()!=200) {
                channel.shutdownNow();
                return new Result(401,reply.getMsg());
            }
        }
        catch (Exception err){
            err.printStackTrace();
            return new Result(500,"错误:"+err.getMessage());
        }

        return new Result(200,"ok");
    }

    public static Result getAccountList(){
        return getAccountList(0);
    }

    //获取专机上账号，如果成功 result.Data = List<String>
    public static Result getAccountList(int zjId) {

        //检测是否成功连接
        Result result = connectGrpcServer();
        if(result.getCode()!=200)
            return result;

        try {

            //发送请求
            AccountListRequest request = AccountListRequest.newBuilder()
                    .setZjId(zjId)
                    .build();

            AccountListReply reply = blockingStub.getAccountList(request);
            if(reply.getCode()!=200) {
                channel.shutdownNow();
                return new Result(reply.getCode(),reply.getMsg());
            }

            List<String> list = new ArrayList<>();
            //结果处理
            for(AccountInfo info: reply.getAccountsList())
            {
                list.add(info.getUsername());
            }

            result.setMsg("获取专机"+ zjId+"账号列表成功");
            result.setData(list);
        }
        catch (Exception err){
            err.printStackTrace();
            return new Result(500,"错误:"+err.getMessage());
        }

        return result;
    }

    //创建账号，如果成功 result.Data = 账号名
    public static Result createAccount(int zjId, String account, String password) {

        //检测是否成功连接
        Result result = connectGrpcServer();
        if(result.getCode()!=200)
            return result;

        try {

            //发送请求
            AccountSetRequest request = AccountSetRequest.newBuilder()
                    .setZjId(zjId)
                    .setUsername(account)
                    .setPassword(password)
                    .build();

            AccountSetReply reply = blockingStub.createAccount(request);
            if(reply.getCode()!=200) {
                channel.shutdownNow();
                return new Result(reply.getCode(),reply.getMsg());
            }

            //结果处理
            result.setMsg("创建专机"+ zjId+"账号成功");
            result.setData(reply.getAccount());
        }
        catch (Exception err){
            err.printStackTrace();
            return new Result(500,"错误:"+err.getMessage());
        }

        return result;
    }

    //记录编辑后的内容通知zjserver
    public static Result sendMessageToZjServer(int action, int zjId, String account, String password,String starttime, String endtime) {

        //检测是否成功连接
        Result result = connectGrpcServer();
        if(result.getCode()!=200)
            return result;

        try {

            //发送请求
            AccountRecordRequest request = AccountRecordRequest.newBuilder()
                    .setZjId(zjId)
                    .setUsername(account)
                    .setPassword(password)
                    .setStarttime(starttime)
                    .setEndtime(endtime)
                    .setAction(action)
                    .build();

            AccountRecordReply reply = blockingStub.recordAccount(request);
            if(reply.getCode()!=200) {
                channel.shutdownNow();
                return new Result(reply.getCode(),reply.getMsg());
            }

            //结果处理
            result.setMsg("创建专机"+ zjId+"账号成功");
            result.setData(reply.getAccount());
        }
        catch (Exception err){
            err.printStackTrace();
            return new Result(500,"错误:"+err.getMessage());
        }

        return result;
    }

    public static Result findOnlineZjList(int action, int zjId, String account, String password,String starttime, String endtime) {

        //检测是否成功连接
        Result result = connectGrpcServer();
        if(result.getCode()!=200)
            return result;

        try {

            //发送请求
            ZjListRequest request = ZjListRequest.newBuilder()
                    .setOnlyOnline(true)
                    .build();

            ZjListReply reply = blockingStub.getZjList(request);
            if(reply.getCode()!=200) {
                channel.shutdownNow();
                return new Result(reply.getCode(),reply.getMsg());
            }

            //结果处理
            result.setMsg("在线专机列表查找成功");
            //result.setData(reply.getZjList());
        }
        catch (Exception err){
            err.printStackTrace();
            return new Result(500,"错误:"+err.getMessage());
        }

        return result;
    }
    //下载文件，如果成功登录目标机成功，开始下载，不代表已经下载完毕
    public static Result downloadFile(DownloadFileRequest request) {

        //检测是否成功连接
        Result result = connectGrpcServer();
        if(result.getCode()!=200)
            return result;

        try {

            int zjId= request.getZjId();

            //发送请求
            DownloadFileReply reply = blockingStub.downloadFile(request);
            if(reply.getCode()!=200) {
                channel.shutdownNow();
                return new Result(reply.getCode(),reply.getMsg());
            }

            //结果处理
            result.setMsg("专机"+ zjId+"开始下载文件成功");
            result.setData(reply.getMsg());
        }
        catch (Exception err){
            err.printStackTrace();
            return new Result(500,"错误:"+err.getMessage());
        }

        return result;
    }

}

