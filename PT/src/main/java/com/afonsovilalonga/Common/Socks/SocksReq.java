package com.afonsovilalonga.Common.Socks;

public class SocksReq{
    private String addr;
    private int port;
    private byte addr_tpye;
    private byte method;

    public SocksReq(String addr, int port, byte addr_tpye, byte method){
        this.addr = addr;
        this.port = port;
        this.addr_tpye = addr_tpye;
        this.method = method;
   } 

   public SocksReq(){

   }

   public String getAddr(){
       return this.addr;
   }

   public void setAdrr(String addr){
       this.addr = addr;
   }

   public int getPort(){
       return this.port;
   }

   public void setPort(int port){
       this.port = port;
   }

   public byte getAddrType(){
       return this.addr_tpye;
   }

   public void setAddrType(byte type){
       this.addr_tpye = type;
   }

   public byte getMethod(){
       return this.method;
   }

   public void setMethod(byte method){
       this.method = method;
   }

}