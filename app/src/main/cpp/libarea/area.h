#ifndef __AREA_IOFO__
#define __AREA_IOFO__
#include "LinkList.h"
//#define MAKEMACHINE_NUMBER()
enum DEVCIE_TYPE{//4?文档未说明
  ROOM_TYPE			  =	1,//室内机
  CELL_DOOR_TYPE  =	2,//栋门口机
  HOUSE_DOOR_TYPE	=	3,//二次确认机
  MONITOR_TYPE		=	5,//围墙机
  GUARD_TYPE			=	6,//保安分机
  AREA_DOOR_TYPE	=	7,//大门口机
  MANAGER_TYPE		=	8,//管理中心
};

//围墙机是什么,目前返回错误
//netItem 是否会有3个,目前仅留了两个0,为正常使用的,1为保留的
//未解析DEVNAME未处理MODEMATCH
//看文档还有些预留选项,目前并未处理
typedef unsigned char IpAddress[4];//IP列表
struct MachineIPInfo{
  IpAddress ipAddress;//起始IP地址
  IpAddress gateWay;//网关
  IpAddress netMask;//掩码
};
struct MachineInfo{
  unsigned char number;//数量
  unsigned char index;//数量大于1时表示是第几台机
  unsigned char ipid;
  unsigned long long machineCode;
  struct MachineIPInfo machineIPInfo;
};
struct Guard{
  unsigned char IPID;
  unsigned char indexBegin;
  unsigned char indexEnd;
};
struct NetItem{
    IpAddress netStart;
    IpAddress netEnd;
    IpAddress netGW;
    int IPID;
};
struct Area{
  int  monitorNUMber;//围墙机数量
  struct Guard guard[3];//0 MANAGE 1 AREAGATE 2 GUARD
  struct NetItem netItem[2];//是否会有多个netItem标签
  unsigned char modeMatch[6];
  //modMatch 6位分别表示区,栋,单元,楼层,房间,分机属性
  //对应位为数字时,表示用几位数字表示,A表示显示为英文字母
  int version;
  LinkList *buildList;//小区栋信息链表
};
struct Build{

  unsigned char IPID;
  unsigned char buildBegin;//楼栋起始编号
  unsigned char buildEnd;//楼栋结束编号
  unsigned char MODID;
  struct Model *model;
  LinkListNode node;
};



struct Cell{//单元信息

  unsigned char cellBegin;//单元起始编号
  unsigned char cellEnd;//单元结束编号
  unsigned char floorBegin;//开始层数
  unsigned char floorEnd;//结束层数
  unsigned char roomBegin;//每层房间起始号
  unsigned char roomEnd;////每层房间结束号
  unsigned char outDoorCount;//门口机数量
  unsigned char inDoorCount;//室内机数量
  unsigned char secondOutDoorCount;//第二门口机数量
  LinkListNode node;
};

struct Model{//栋信息
  unsigned char id;
  LinkList *cellList;//小区单元信息链表
  LinkListNode node;
};


//设备类型,机器号,房间号等生成machineCode
#define GENERATE_MACHINE_CODE(machineIndex,\
        subMachineIndex,\
        doorIndex,\
        floorIndex,\
        cellIndex,\
        buildIndex,\
        areaIndex\
)                    ((machineIndex  & 0x0f)   |/*设备型号.室内机为1*/\
                      (subMachineIndex  << 4 ) |/*机器号1,2,3...*/\
                      (doorIndex  << 12 )      |/*房间号*/\
                      (floorIndex  << 20 )     |/*楼层号*/\
                      (cellIndex << 28 )       |/*单元号*/\
                      ((unsigned long long)(buildIndex) << 40 )   |/*楼栋号*/\
                      ((unsigned long long)(areaIndex)  << 52 )    /*区号*/\
                    )
//解析machineCode 为区,单元等信息
#define PRASE_MACHINE_CODE(machineIndex,\
        subMachineIndex,\
        doorIndex,\
        floorIndex,\
        cellIndex,\
        buildIndex,\
        areaIndex,\
        machineCode\
)                       do{ machineIndex = machineCode & 0x0f;\
                            subMachineIndex = (machineCode >>4 )& 0xff;\
                            doorIndex = (machineCode >>12 )& 0xff;\
                            floorIndex = (machineCode >>20 )& 0xff;\
                            cellIndex = (machineCode >>28 )& 0xfff;\
                            buildIndex = (machineCode >>40 )& 0xfff;\
                            areaIndex = (machineCode >>52 )& 0xff;\
                    }while(0)
//依据文档名获取整个小区的信息
struct Area* getAreaInfo(char *xmlDocName);
//释放小区信息
void freeArea(struct Area *area);
//依据machineCode获取ip地址网关及掩码,machineCode从设置界面获取
//返回0表示参数设置错误
int initMachineInfo(unsigned long long machineCode, struct Area *area, struct MachineInfo *machineInfo);
//从本机的srcMachineInfo 生成目标机的destMachineCode,type为目标机类型,返回值为目标机个数
//对应关系为子可寻父,父不可寻子,兄弟也可以寻比如室内机的srcMachineCode可以获取到区的门口机,二次确认机及管理中心,保安分级及,但是管理中心的srcMachineCode无法寻找区的门口机
//destMach为室内机时,需指定栋,单元,楼层,房间
int getObjectMachineInfo(unsigned long long *destMachineCode,unsigned long long srcMachineCode,struct Area * area, enum DEVCIE_TYPE type);
#endif
