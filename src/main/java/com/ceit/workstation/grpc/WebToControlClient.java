//package com.ceit.workstation.grpc.client;
//
//import com.ceit.bootstrap.ConfigLoader;
//import com.ceit.workstation.grpc.grpcjava.WebGrpc;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//
//public class WebToControlClient {
//    //web通过grpc请求管控中心
//    public WebGrpc.WebBlockingStub blockingStub;
//
//    public WebToControlClient() {
//        //String ip = System.getProperty("web.grpc.ip");
//        String ip = ConfigLoader.getConfig("web.grpc.ip");
//        int port = Integer.valueOf(ConfigLoader.getConfig("web.grpc.port"));
//        //int port = Integer.valueOf(System.getProperty("web.grpc.port"));
//        ManagedChannel channel = ManagedChannelBuilder
//                .forAddress(ip,port)
//                .usePlaintext()
//                .build();
//        blockingStub = WebGrpc.newBlockingStub(channel);
//    }
//}
