#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <malloc.h>
#include <stdlib.h>
#include <inttypes.h>

#include <android/log.h>
#include <unistd.h>
// log标签
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#ifdef __cplusplus
extern "C"
{
#endif
#include "area.h"
#define TAG "area"
#define XMLPATH "/data/local/tmp/netcfg.xml"
char* test(int floor, int roomId) // 房间号，楼层号

{

    struct Area *area = getAreaInfo(XMLPATH);

    IpAddress ipaddress;

    unsigned long long machineCode; /*= (6  & 0x0f) | //设备型号.室内机为1
      (3  << 4 ) | //机器号1,2,3...
      (1  << 12 ) | //房间号
      (1  << 20 ) |  //楼层号
      (1ULL << 28 ) |//单元号
      (7ULL << 40 )|//楼栋号
      (1ULL  << 52 );//区号*/
    // LOGI("roomId %d",roomId);
    machineCode = GENERATE_MACHINE_CODE(1,2,roomId,floor,1,5,1);
    LOGI("machineCOde %llx\n",machineCode);
    struct MachineInfo machineInfo ;
    memset(&machineInfo,0,sizeof(machineInfo));
    int machineNumber;
    //在整个小区中为第几台机
    machineNumber = initMachineInfo(machineCode,area,&machineInfo);
    LOGI("machine number %d\n",machineNumber);
    if(machineNumber > 0){
        LOGI("ip %d:%d:%d:%d\n",(machineInfo.machineIPInfo.ipAddress)[0],
               (machineInfo.machineIPInfo.ipAddress)[1],
               (machineInfo.machineIPInfo.ipAddress)[2],
               (machineInfo.machineIPInfo.ipAddress)[3]);

        LOGI("gateway: %d:%d:%d:%d\n",(machineInfo.machineIPInfo.gateWay)[0],
               (machineInfo.machineIPInfo.gateWay)[1],
               (machineInfo.machineIPInfo.gateWay)[2],
               (machineInfo.machineIPInfo.gateWay)[3]);

        LOGI("netmask: %d:%d:%d:%d\n",(machineInfo.machineIPInfo.netMask)[0],
               (machineInfo.machineIPInfo.netMask)[1],
               (machineInfo.machineIPInfo.netMask)[2],
               (machineInfo.machineIPInfo.netMask)[3]);

        unsigned long long destMachineCode;
        //返回目标机器个数
        int objectNum = getObjectMachineInfo(&destMachineCode,machineCode,area,ROOM_TYPE);
        LOGI("object machine number:%d machineCOde %llx\n",objectNum,destMachineCode);
        struct MachineInfo machineInfoTest;
        IpAddress ipAddress = {0};

        machineNumber = initMachineInfo(destMachineCode,area,&machineInfoTest);
        memcpy(&ipAddress[0] ,&((machineInfoTest.machineIPInfo.ipAddress)[0]),4);
        LOGI("machine number %d\n",machineNumber);

        if(machineNumber > 0){
            int i = 0;
            //打印所有目标机ip信息
            for(i = 0 ;i < objectNum;i++){
                if((ipAddress[3] + i) / 255){
                    ipAddress[2] += 1;
                    ipAddress[3] =  (ipAddress[3] + i)%255 + 1;

                }else{
                    ipAddress[3] += (i == 0) ?0:1;
                }
                LOGI("ip %d:%d:%d:%d\n",(ipAddress)[0],
                       (ipAddress)[1],
                       (ipAddress)[2],
                       (ipAddress)[3]);
            }
        }

         char* a= "返回值";
         // sprintf(a,"12323%d", 22);// (machineInfo.machineIPInfo.ipAddress)[0]);
                           //(machineInfo.machineIPInfo.ipAddress)[1],
                           //(machineInfo.machineIPInfo.ipAddress)[2],
                           //(machineInfo.machineIPInfo.ipAddress)[3]);
         return a;
        // return (machineInfo.machineIPInfo.gateWay)[0];

    }

    freeArea(area);
    return 0;
    //return sprintf("ddd %d",1233);
}

JNIEXPORT jstring JNICALL
Java_com_test_door_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    // int ii = test(1,1);
    std::string hello = "Hello from C++ ";

    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_test_door_MainActivity_roomIdGetIP(
    JNIEnv *env,
    jobject jobj, jint floor, jint roomId) {

    char ipList[50][16]={'\0'};
    struct Area *area = getAreaInfo(XMLPATH);

    IpAddress ipaddress;

    unsigned long long machineCode; /*= (6  & 0x0f) | //设备型号.室内机为1
      (3  << 4 ) | //机器号1,2,3...
      (1  << 12 ) | //房间号
      (1  << 20 ) |  //楼层号
      (1ULL << 28 ) |//单元号
      (7ULL << 40 )|//楼栋号
      (1ULL  << 52 );//区号*/
    // LOGI("roomId %d",roomId);
    machineCode = GENERATE_MACHINE_CODE(1,1,roomId,floor,1,1,1);
    LOGI("machineCOde %llx\n",machineCode);
    struct MachineInfo machineInfo ;
    memset(&machineInfo,0,sizeof(machineInfo));
    int machineNumber;
    //在整个小区中为第几台机
    machineNumber = initMachineInfo(machineCode,area,&machineInfo);
    LOGI("machine number %d\n",machineNumber);
    if(machineNumber > 0){
        LOGI("local ip %d:%d:%d:%d\n",(machineInfo.machineIPInfo.ipAddress)[0],
               (machineInfo.machineIPInfo.ipAddress)[1],
               (machineInfo.machineIPInfo.ipAddress)[2],
               (machineInfo.machineIPInfo.ipAddress)[3]);

        LOGI("gateway: %d:%d:%d:%d\n",(machineInfo.machineIPInfo.gateWay)[0],
               (machineInfo.machineIPInfo.gateWay)[1],
               (machineInfo.machineIPInfo.gateWay)[2],
               (machineInfo.machineIPInfo.gateWay)[3]);

        LOGI("netmask: %d:%d:%d:%d\n",(machineInfo.machineIPInfo.netMask)[0],
               (machineInfo.machineIPInfo.netMask)[1],
               (machineInfo.machineIPInfo.netMask)[2],
               (machineInfo.machineIPInfo.netMask)[3]);

        unsigned long long destMachineCode;
        //返回目标机器个数
        int objectNum = getObjectMachineInfo(&destMachineCode,machineCode,area,ROOM_TYPE);
        LOGI("object machine number:%d machineCOde %llx\n",objectNum,destMachineCode);
        struct MachineInfo machineInfoTest;
        IpAddress ipAddress = {0};

        machineNumber = initMachineInfo(destMachineCode,area,&machineInfoTest);
        memcpy(&ipAddress[0] ,&((machineInfoTest.machineIPInfo.ipAddress)[0]),4);
        LOGI("machine number %d\n",machineNumber);

        if(machineNumber > 0){
            int i = 0;
            //打印所有目标机ip信息
            for(i = 0 ;i < objectNum;i++){
                if((ipAddress[3] + i) / 255){
                    ipAddress[2] += 1;
                    ipAddress[3] =  (ipAddress[3] + i)%255 + 1;

                }else{
                    ipAddress[3] += (i == 0) ?0:1;
                }
                LOGI("ip %d:%d:%d:%d\n",(ipAddress)[0],
                       (ipAddress)[1],
                       (ipAddress)[2],
                       (ipAddress)[3]);
                char name[16];
                memset(name, 0, 16);
                sprintf(name, "%d.%d.%d.%d", (ipAddress)[0], (ipAddress)[1],
                						(ipAddress)[2], (ipAddress)[3]);
                memcpy(ipList[i],name,sizeof(char)*16);
                LOGI("machine dest ip:%s\n", ipList[i]);
            }
        }

    return  env->NewStringUTF(ipList[0]);
}
}
#ifdef __cplusplus
}
#endif
