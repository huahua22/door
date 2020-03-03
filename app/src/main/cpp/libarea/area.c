
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <malloc.h>
#include <stdlib.h>
#include <libxml/parser.h>
#include <libxml/tree.h>
#include "LinkList.h"
#include "area.h"
#define ARRAYL_LENGTH(array) (sizeof(array)/sizeof(array[0]))
const char * const cellInfo[] = {
  "CellBegin","CellEnd",
	"FloorBegin","FloorEnd",
	"RoomBegin","RoomEnd",
	"DoorCount",
	"RoomCount",
  "SecondCount"};
const char * const buildInfo[] = {
    "IPID",
  	"BUILDBEGIN",
  	"BUILDEND",
  	"MODID"};

const char * const guardName[] = {//管理分级,保安机,围墙机信息一致,使用名字作区分
  "MANAGE",
   "AREAGATE",
   "GUARD"
  };
const char * const guardInfo[] = {
  "IPID",
  "IndexBegin",
  "IndexEnd",
};

const char * const netItemInfo[] = {
  "NETSTART",
  "NETEND",
  "NETGW",
  "IPID",
};


const char * const areaName[] = {
  "Modol",
  "BuildList",
  "MANAGE",
  "AREAGATE",
  "GUARD",
  "NETITEM",
  "DEVNAME",
  "MODEMATCH",
  "VERSION",
  "NETITEM",
};

//此函数要求传入结构体中数据元素大小必须一致
int fillInfo(const char * const info[],int length,xmlNodePtr node,unsigned char * contentAddr,int size){

  int i;
  int keyIndex = 0;//匹配完整标志
  if(node == NULL)
    return keyIndex;
  for(i = 0;i < length ;i++){

    xmlNodePtr subNode = node->xmlChildrenNode;
    while(subNode != NULL  && xmlStrcmp(subNode->name, (const xmlChar *) info[i]) ){
      subNode = subNode->next;

    }
    if(subNode == NULL){//子node中缺省info中某一项
      contentAddr += size;
      continue;
    }


    if(!xmlStrcmp(subNode->name, (const xmlChar *) info[i])){
        xmlChar* szKey = xmlNodeGetContent(subNode->xmlChildrenNode);
        if(szKey != NULL){
          if(xmlStrncmp(subNode->name, (const xmlChar *)"NET",3) == 0)//包含"NET子串的做特殊处理,其他转换为数字"
          {




            char szKeyCopy[32];
            strcpy(szKeyCopy,szKey);
            char *str = strtok(szKeyCopy,".");
            int j ;
            for(j = 0;j < 4 && str != NULL;j++){
              unsigned char index;
              sscanf(str,"%d",&index);
              //低位在前.高位在后
              (*(IpAddress *)contentAddr)[j] = index;
              str = strtok(NULL,".");

            }
            //指针加法
            contentAddr = (char *)((IpAddress *)contentAddr + 1);

          }else{
            int index;
            sscanf(szKey,"%d",&index);
            *(int *)contentAddr++ = index;

          }
          keyIndex++;
          xmlFree(szKey);
        }


    }


  }
  return keyIndex;
}

struct Area* getAreaInfo(char *xmlDocName){
  xmlDocPtr doc;           //定义解析文档指针
  xmlNodePtr curNode;      //定义结点指针(你需要它为了在各个结点间移动)
  xmlKeepBlanksDefault(0);//libxml 会将空格认为一个节点,name为text
  doc = xmlReadFile(xmlDocName,"UTF-8",XML_PARSE_RECOVER); //解析文件
  //检查解析文档是否成功，如果不成功，libxml将指一个注册的错误并停止。
  //一个常见错误是不适当的编码。XML标准文档除了用UTF-8或UTF-16外还可用其它编码保存。
  //如果文档是这样，libxml将自动地为你转换到UTF-8。更多关于XML编码信息包含在XML标准中.
  if (NULL == doc)
  {
     fprintf(stderr,"%s","Document not parsed successfully\n");
     return NULL;
  }

  curNode = xmlDocGetRootElement(doc); //确定文档根元素
  /*检查确认当前文档中包含内容*/
  if (NULL == curNode)
  {
     fprintf(stderr,"empty document\n");
     xmlFreeDoc(doc);
     return NULL;
  }

  /*在这个例子中，我们需要确认文档是正确的类型。“root”是在这个示例中使用文档的根类型。*/

  if (xmlStrcmp(curNode->name, BAD_CAST "NETCFG"))
  {
     fprintf(stderr,"document of the wrong type, root node != root");
     xmlFreeDoc(doc);
     return NULL;
  }
  curNode = curNode->xmlChildrenNode;

  struct Area *area = malloc(sizeof(struct Area));
  if(area == NULL){
    xmlFreeDoc(doc);
    return NULL;
  }
  memset(area,0,sizeof(struct Area));

  area->buildList = linklist_create();


  LinkList *modelList = linklist_create();

  int netItemIndex = 0;//netItem 共有两个,用0,1表示
  while(curNode != NULL)
  {

    int i = 0;
    for(i = 0;i < ARRAYL_LENGTH(areaName);i++){
      if((!xmlStrcmp(curNode->name, (const xmlChar *)areaName[i])))//寻找匹配项
        break;
    }

    switch(i){
      case 0:{
        xmlNodePtr subNode = curNode->xmlChildrenNode;
        struct Model *model = malloc(sizeof(struct Model));


        if(model != NULL && subNode != NULL){
          memset(model,0,sizeof(struct Model));
          if ((!xmlStrcmp(subNode->name, (const xmlChar *)"ID")))
          {
            xmlChar* szKey = xmlNodeGetContent(subNode);
            sscanf(szKey,"%d",&model->id);
            xmlFree(szKey);
          }

          model->cellList  = linklist_create();
          while(subNode != NULL){

            if (!xmlStrcmp(subNode->name, (const xmlChar *)"Item")){
              struct Cell *cell = malloc(sizeof(struct Cell));
              memset(cell,0,sizeof(struct Cell));
              fillInfo(cellInfo,sizeof(cellInfo)/sizeof(cellInfo[0]),subNode,&cell->cellBegin,sizeof(cell->cellBegin));

              linklist_insert(model->cellList,linklist_length(model->cellList),&cell->node);

            }
            subNode = subNode->next;
          }

          linklist_insert(modelList,linklist_length(modelList),&model->node);
        }else{
          free(model);
          model = NULL;
        }

        break;
      }

      case 1:
          {

            int i = 0;
            struct Build  *build = malloc(sizeof(struct Build));
            if(build == NULL)
              break;
            memset(build,0,sizeof(*build));
            fillInfo(buildInfo,sizeof(buildInfo)/sizeof(buildInfo[0]),curNode,&build->IPID,sizeof(build->IPID));


            for(i = 0;i < linklist_length(area->buildList);i++){
              struct Build  *existBuild = container_of(linklist_get(area->buildList,i),struct Build,node);
              if(existBuild->MODID == build->MODID){//两栋楼有相同单元信息,buildEnd增加即可
                  existBuild->buildEnd++;
                  break;
              }
            }
            if(i == linklist_length(area->buildList)){
              //已经建立的楼栋中不存在与当前楼栋单元一致的
              int i = 0;
              for(i = 0;i < linklist_length(modelList);i++){
                struct Model  *model = container_of(linklist_get(modelList,i),struct Model,node);
                if(build->MODID == model->id){//MODID与id匹配,表明单元属于该栋
                  build->model = model;
                  linklist_insert(area->buildList,linklist_length(area->buildList),&build->node);
                  break;
                }
              }


            }else{
              free(build);
              build = NULL;
            }


            break;
          }

      case 2:case 3:case 4:
        //2  "MANAGE",3  "AREAGATE",4  "GUARD"
        fillInfo(guardInfo,ARRAYL_LENGTH(guardInfo),curNode,&area->guard[i - 2].IPID,sizeof(area->guard[i - 2].IPID));//manage areagate guard 信息类似,放在一起处理
        break;
      case 5:

        fillInfo(netItemInfo,ARRAYL_LENGTH(netItemInfo),curNode,(unsigned char * const)&area->netItem[netItemIndex].netStart,sizeof(area->netItem[netItemIndex++].netStart));
        IpAddress *ipStart = &(area->netItem[netItemIndex].netStart);
        IpAddress *netGW = &(area->netItem[netItemIndex].netGW);
        if((*netGW)[0] != 0){//默认有网关时,ip起始地址从网关下一地址算
          if((*netGW)[3] >= (*ipStart)[3])
            (*ipStart)[3] = (*netGW)[3];
        }
        netItemIndex++;
        break;
      case 6:
        break;
      case 7:{
        xmlChar* szKey = xmlNodeGetContent(curNode);
        if(szKey != NULL){
          int i;
          for(i = 0;i < ARRAYL_LENGTH(area->modeMatch);i++){//0~9 及A
            area->modeMatch[i] = szKey[ARRAYL_LENGTH(area->modeMatch) - i - 1] - 48;

          }
          xmlFree(szKey);
        }
        break;
      }

      case 8:{
        xmlChar* szKey = xmlNodeGetContent(curNode);
        if(szKey != NULL){
          sscanf(szKey,"%d",&area->version);
          xmlFree(szKey);
        }
        break;
      }

      case 9:
        break;
      default:
        printf("unknow info\n");
        break;
    }
    curNode = curNode->next;


  }
  linklist_destroy(modelList);


  xmlFreeDoc(doc);
  return area;
}


void freeArea(struct Area *area)
{
  int i ;
  if(area != NULL){
    while(linklist_length(area->buildList) > 0){

      struct Build  *build = container_of(linklist_deleate(area->buildList,0),struct Build,node);
      if(build != NULL){
        for(i = 0;i < sizeof (buildInfo)/sizeof buildInfo[0];i++){
        //  printf("#%s %d\n",buildInfo[i],(&build -> IPID)[i] );
        }
        while(linklist_length(build->model->cellList) > 0)
        {
          struct Cell  *cell = container_of(linklist_deleate(build->model->cellList,0),struct Cell,node);
          if(cell != NULL){
            for(i = 0;i < sizeof cellInfo/sizeof cellInfo[0];i++){
            //  printf("#######%s %d\n",cellInfo[i],(&cell -> cellBegin)[i] );
            }
            free(cell);
            cell = NULL;
          }
        }
        linklist_destroy(build->model->cellList);

        free(build->model);
        build->model = NULL;
        free(build);
      }

    }

    linklist_destroy(area->buildList);
    for(i = 0;i < ARRAYL_LENGTH(area->guard);i++){
      //printf("#######name:%s \n",guardName[i]);
      int j ;
      for(j = 0 ;j < ARRAYL_LENGTH(guardInfo);j++){
        // printf("#############:%s %d\n",guardInfo[j],*(&area->guard[i].IPID + j));
      }

    }

    for(i = 0;i < ARRAYL_LENGTH(area->modeMatch);i++){
      //printf("#######name:%d \t \n",area->modeMatch[ARRAYL_LENGTH(area->modeMatch) - i - 1]);

    }
    for(i = 0;i < ARRAYL_LENGTH(area->netItem);i++){
      int j ,k;
      for(j = 0;j < 3;j++){
      //  printf("\n#######netItem:%d \t \n",j);
        for(k = 0;k < 4;k++){
        //  printf(":%d \t ",(*(((ipAddress *)(area->netItem[i].netStart)) + j))[k]);
        }

      }


    //  printf("\n#######netIPID:%d \t \n",area->netItem[i].IPID);
    }

      free(area);
      area = NULL;
  }

}
//计算某单元机器总数

int calculationCellMachineNumber(struct Cell  *cell){
   int machineNumber = 0;
   if(cell != NULL){
     machineNumber =
                        (
                          (cell->floorEnd - cell->floorBegin + 1) *\
                          (cell->roomEnd - cell->roomBegin + 1) *\
                          (cell->inDoorCount + cell->secondOutDoorCount) +\
                          cell->outDoorCount \
                        );

   }
   return machineNumber;
}

//计算栋机器总数
int calculationBuildMachineNumber(struct Build  *build){
   int machineNumber = 0;
   if(build != NULL){
     struct Cell *cell ;
     int index = 0;
     for(index = 0;index < linklist_length(build->model->cellList);index++){
       cell = container_of(linklist_get(build->model->cellList,index),struct Cell,node);
       if(cell != NULL)
        machineNumber += (cell->cellEnd - cell->cellBegin + 1) * calculationCellMachineNumber(cell);
     }

   }
   return machineNumber;
}
int calculationIP(struct Area  *area,int machineNumber,int ipid,struct MachineIPInfo *machineIPInfo){
  int ret = -1;
  if(area != NULL && machineNumber > 0  && machineIPInfo != NULL){
    int i = 0;
    for(i = 0 ;i < ARRAYL_LENGTH(area->netItem);i++){
      if(ipid == area->netItem[i].IPID){
          ret = 0;
          IpAddress *ipStart = &(area->netItem[i].netStart);
          IpAddress *ipEnd= &(area->netItem[i].netEnd);
          IpAddress *ipGw= &(area->netItem[i].netGW);


        unsigned char h8 = (machineNumber+(*ipStart)[3]) / 255;
        unsigned char l8 = (h8 == 0)?machineNumber+(*ipStart)[3]:(machineNumber+(*ipStart)[3]) % 255 + h8*1;//ip低段不会为0,所以需加上255倍数的1
        if(l8 %255 == 0)
          l8 = 1;
        (machineIPInfo->ipAddress)[0] = (*ipStart)[0];
        (machineIPInfo->ipAddress)[1] = (*ipStart)[1];
        (machineIPInfo->ipAddress)[2] = (*ipStart)[2] + h8;
        (machineIPInfo->ipAddress)[3] = l8;

        memcpy(machineIPInfo->gateWay,*ipGw,ARRAYL_LENGTH((*ipGw)));

        machineIPInfo->netMask[3] = 0;
        int j = 0;
        for(j = 2;j >= 0;j--){
          if((*ipStart)[j] != (*ipEnd)[j])
            machineIPInfo->netMask[j] = 256 - (*ipEnd)[j];
          else
            machineIPInfo->netMask[j] = 255;
        }

        //printf("id %d:%d:%d:%d\n",(machineIPInfo->ipAddress)[0],(machineIPInfo->ipAddress)[1],(machineIPInfo->ipAddress)[2],(machineIPInfo->ipAddress)[3]);
      }
    }
  }
    return ret;
}
/*
通过srcMachineInfo及传入的type来获取目标的machineCode 返回值为目标个数,有多个目标时destMachineCode为首个目标的destMachineCode
*/
int getObjectMachineInfo(unsigned long long *destMachineCode,unsigned long long srcMachineCode,struct Area * area, enum DEVCIE_TYPE type){
  int num = 0;
  unsigned char  machineIndex ;
  unsigned char  subMachineIndex ;
  unsigned char  doorIndex ;
  unsigned char  floorIndex ;
  unsigned int  cellIndex ;
  unsigned int  buildIndex ;
  unsigned int  areaIndex ;

  PRASE_MACHINE_CODE(machineIndex,subMachineIndex,doorIndex,\
                    floorIndex,cellIndex,buildIndex,areaIndex,\
                    srcMachineCode);

  //源信息为围墙机,保安分级,大门口机及管理中心时无法生成室内机及门口机信息
  if(machineIndex == GUARD_TYPE || machineIndex == MONITOR_TYPE || machineIndex == AREA_DOOR_TYPE || machineIndex == MANAGER_TYPE){
    if(type == ROOM_TYPE || type == CELL_DOOR_TYPE || HOUSE_DOOR_TYPE){
      num = 0;
      return num;
    }

  }
  //源信息为门口机时无法生成室内机及门口机信息
  if(machineIndex == CELL_DOOR_TYPE || machineIndex == HOUSE_DOOR_TYPE ){
    if(type == ROOM_TYPE ){
      num = 0;
      return num;
    }

  }

  if(area != NULL ){
    subMachineIndex = 1;
    machineIndex = type;
    //获取目标为围墙机,保安分级,大门口机及管理中心时,直接将楼栋,单元,楼层,房号置0,同时数量可直接从area获取
    if(type == GUARD_TYPE || type == MONITOR_TYPE || type == AREA_DOOR_TYPE || type == MANAGER_TYPE){

      doorIndex = 1;//区相关的机器设置为1栋,1单元,1楼,1房
      floorIndex = 1;
      cellIndex = 1;
      buildIndex= 1;
      switch(type){
          case MONITOR_TYPE	://围墙机
            break;
          case GUARD_TYPE://保安分机
            num = area->guard[2].indexEnd - area->guard[2].indexBegin + 1;
            break;
          case AREA_DOOR_TYPE	://大门口机
            num = area->guard[1].indexEnd - area->guard[1].indexBegin + 1;
            break;
          case MANAGER_TYPE	://管理中心
            num = area->guard[0].indexEnd - area->guard[0].indexBegin + 1;
            break;
          default :
            printf("unknow choice\n");
            break;
      }

    }else{//机器为门口机,二次确认机,室内机
      struct Build *build ;
      struct Cell *cell ;
      int i;
      //找符合buildIndex的build
      for(i = 0;i< linklist_length(area->buildList);i++){
        build = container_of(linklist_get(area->buildList,i),struct Build,node);
        if(build != NULL ){
          if(buildIndex > build->buildEnd){
            continue;
          }else if(buildIndex <= build->buildEnd && buildIndex <= build->buildBegin){
            break;
          }
        }
      }
      //找符合cellIndex的build
      for(i = 0;i< linklist_length(build->model->cellList);i++){
        cell = container_of(linklist_get(build->model->cellList,i),struct Cell,node);
        if(cell != NULL ){
          if(cellIndex > cell->cellEnd){
            continue;
          }else if(cellIndex <= cell->cellEnd && cellIndex >= cell->cellBegin){
            switch(type){
              case ROOM_TYPE://室内机,为室内机时必须确保楼层及房间号存在

                if(floorIndex >= cell->floorBegin && floorIndex <= cell->floorEnd &&
                    doorIndex >= cell->roomBegin && doorIndex <= cell->roomEnd)
                  num = cell->inDoorCount;
              	break;
              case CELL_DOOR_TYPE: //门口机
                floorIndex = 1;
                doorIndex = 1;
                num = cell->outDoorCount;
                break;
              case HOUSE_DOOR_TYPE:
                num = cell->secondOutDoorCount;
                break;
              default :
                printf("unknow choice\n");
                num = 0;
                break;
            }
            break;
          }
        }
      }

    }


  }
  *destMachineCode = GENERATE_MACHINE_CODE(machineIndex,subMachineIndex,doorIndex,\
                    floorIndex,cellIndex,buildIndex,areaIndex);
  return num;
}
int initMachineInfo(unsigned long long machineCode, struct Area *area, struct MachineInfo *machineInfo){

  //ipid判断处于同一局域网,机器数才增加
  //区 8bit
  //楼 12bit
  //单元 12bit
  //层 8bit
  //房 8bit
  //分机 8bit
  //终端 4bit

  unsigned char  machineIndex ;
  unsigned char  subMachineIndex ;
  unsigned char  doorIndex ;
  unsigned char  floorIndex ;
  unsigned int  cellIndex ;
  unsigned int  buildIndex ;
  unsigned int  areaIndex ;

  PRASE_MACHINE_CODE(machineIndex,subMachineIndex,doorIndex,\
                    floorIndex,cellIndex,buildIndex,areaIndex,\
                    machineCode);
    int machineNumber = 0;
    if(machineInfo != NULL && area != NULL){

      switch(machineIndex){//参数合法性判断
        case ROOM_TYPE :/*室内机*/case CELL_DOOR_TYPE:/*栋门口机*/case HOUSE_DOOR_TYPE://二次确认机
          {

            struct Build  *build;
            struct Cell *cell;
            int i;
            for(i = 0;i< linklist_length(area->buildList);i++){
              build = container_of(linklist_get(area->buildList,i),struct Build,node);
              if(build != NULL){
                if(buildIndex > build->buildEnd){//未到达所在栋,继续寻找
                    continue;
                }else if(buildIndex >= build->buildBegin && areaIndex <= build->buildEnd){
                    break;
                }else if(buildIndex < build->buildBegin){
                    break;
                }
              }
            }

            if(build == NULL || buildIndex < build->buildBegin || buildIndex > build->buildEnd)
              return 0;


            for(i = 0;i < linklist_length(build->model->cellList);i++){
                cell = container_of(linklist_get(build->model->cellList,i),struct Cell,node);
                if(cellIndex > cell->cellEnd){//未到达所在单元,直接计算机器数
                  continue;
                }else if(cellIndex >= cell->cellBegin && cellIndex <= cell->cellEnd){
                  break;
                }else if(cellIndex < cell->cellBegin){
                  break;
                }
            }

            if(cell == NULL || cellIndex < cell->cellBegin || cellIndex > cell->cellEnd)
              return 0;

            if(machineIndex == CELL_DOOR_TYPE){

              if(subMachineIndex > cell->outDoorCount || subMachineIndex < 1)
                return 0;
            }else if(machineIndex == HOUSE_DOOR_TYPE){
              if(floorIndex < cell->floorBegin || floorIndex > cell->floorEnd)
                return 0;

              if(doorIndex < cell->roomBegin || doorIndex > cell->roomEnd)
                return 0;
              if(subMachineIndex > cell->secondOutDoorCount || subMachineIndex < 1)
                return 0;
            }else if(machineIndex == ROOM_TYPE){

              if(floorIndex < cell->floorBegin || floorIndex > cell->floorEnd)
                return 0;

              if(doorIndex < cell->roomBegin || doorIndex > cell->roomEnd)
                return 0;
              if(subMachineIndex < 1 || subMachineIndex > cell->inDoorCount)
                return 0;
            }

          }

          break;
        case MONITOR_TYPE	://围墙机
          return 0;
        case GUARD_TYPE://保安分机
          if(subMachineIndex < area->guard[2].indexBegin ||subMachineIndex > area->guard[2].indexEnd)
            return 0;
          break;
        case AREA_DOOR_TYPE	://大门口机
          if(subMachineIndex < area->guard[1].indexBegin ||subMachineIndex > area->guard[1].indexEnd)
            return 0;
          break;
        case MANAGER_TYPE	://管理中心
          if(subMachineIndex < area->guard[0].indexBegin ||subMachineIndex > area->guard[0].indexEnd)
            return 0;
          break;
        default :
            return 0;
      }

      machineInfo->machineCode = machineCode;
      if(machineIndex == MONITOR_TYPE ||\
        machineIndex == GUARD_TYPE ||\
        machineIndex == AREA_DOOR_TYPE ||\
        machineIndex == MANAGER_TYPE \
      ){//判断为围墙机,保安分级,大门口机或管理中心机,直接计算到最后一个房间的所有机器数

        switch(machineIndex){
          case MONITOR_TYPE:
            break;
          case GUARD_TYPE:
            machineInfo->ipid = area->guard[2].IPID;
            machineInfo->number = area->guard[2].indexEnd - area->guard[2].indexBegin + 1;
            break;
          case AREA_DOOR_TYPE:
            machineInfo->ipid = area->guard[1].IPID;
            machineInfo->number = area->guard[1].indexEnd - area->guard[1].indexBegin + 1;
            break;
          case MANAGER_TYPE:
            machineInfo->ipid = area->guard[0].IPID;
            machineInfo->number = area->guard[0].indexEnd - area->guard[0].indexBegin + 1;
            break;
          default :
            printf("unknow device\n");
        }
        machineInfo->index = subMachineIndex;

        struct Build  *build;
        int i;
        for(i = 0;i< linklist_length(area->buildList);i++){
          build = container_of(linklist_get(area->buildList,i),struct Build,node);
          if(build != NULL && build->IPID == machineInfo->ipid){
            machineNumber += (build->buildEnd - build->buildBegin + 1) * calculationBuildMachineNumber(build);
          }
        }

        switch(machineIndex){
          case MONITOR_TYPE:

            break;
          case GUARD_TYPE:

            machineNumber += area->guard[0].indexEnd - area->guard[0].indexBegin +1  + \
                             area->guard[1].indexEnd - area->guard[1].indexBegin +1 + \
                             subMachineIndex;
            break;
          case AREA_DOOR_TYPE:
            machineNumber += area->guard[0].indexEnd - area->guard[0].indexBegin +1 + \
                           subMachineIndex;
            break;
          case MANAGER_TYPE:
            machineNumber += subMachineIndex;
            break;
          default :
            printf("unknow device\n");
        }

      }else{
        {
          int i = 0;
          struct Build  *build;

          for(i = 0;i< linklist_length(area->buildList);i++){
            build = container_of(linklist_get(area->buildList,i),struct Build,node);
            if(build != NULL){
              if(buildIndex > build->buildEnd){//未到达所在栋,继续寻找
                  continue;
              }else if(buildIndex >= build->buildBegin && areaIndex <= build->buildEnd){
                  machineInfo->ipid = build->IPID;
                  break;
              }
            }
          }
          for(i = 0;i< linklist_length(area->buildList);i++){
            build = container_of(linklist_get(area->buildList,i),struct Build,node);

            if(build != NULL){
              if(build->IPID == machineInfo->ipid){
                if(buildIndex > build->buildEnd){//未到达所在栋,直接计算机器数
                    machineNumber += (build->buildEnd - build->buildBegin + 1) * calculationBuildMachineNumber(build);
                }else if(buildIndex >= build->buildBegin && areaIndex <= build->buildEnd){

                  machineNumber += (buildIndex - build -> buildBegin) * calculationBuildMachineNumber(build);
                  //int index = 0;

                  struct Cell *cell = NULL;
                  int j;
                  for(j = 0;j < linklist_length(build->model->cellList);j++){
                    cell = container_of(linklist_get(build->model->cellList,j),struct Cell,node);
                    if(cellIndex > cell->cellEnd){//未到达所在单元,直接计算机器数
                      machineNumber +=  (cell->cellEnd - cell->cellBegin + 1) * calculationCellMachineNumber(cell);
                    }else if(cellIndex >= cell->cellBegin && cellIndex <= cell->cellEnd){
                      machineNumber += (cellIndex - cell->cellBegin) * calculationCellMachineNumber(cell);
                      switch(machineIndex){
                        case ROOM_TYPE:

                          machineNumber +=   cell->outDoorCount +\
                            (floorIndex - cell->floorBegin ) *\
                            (cell->roomEnd - cell->roomBegin + 1) *\
                            (cell->inDoorCount + cell->secondOutDoorCount ) +\
                            (doorIndex - cell->roomBegin ) * (cell->inDoorCount + cell->secondOutDoorCount ) + \
                            cell->secondOutDoorCount +\
                            subMachineIndex;//机器从1号计数

                          machineInfo->number = cell->inDoorCount;
                          machineInfo->index = subMachineIndex;
                          break;
                        case CELL_DOOR_TYPE:
                          machineNumber += subMachineIndex;
                          machineInfo->number = cell->outDoorCount;
                          machineInfo->index = subMachineIndex;
                          break;
                        case HOUSE_DOOR_TYPE:
                          machineNumber +=   cell->outDoorCount +\
                            (floorIndex - cell->floorBegin ) * (cell->roomEnd - cell->roomBegin + 1) *\
                              (cell->inDoorCount + cell->secondOutDoorCount ) +\
                            (doorIndex - cell->roomBegin ) * (cell->inDoorCount + cell->secondOutDoorCount ) + \
                            subMachineIndex;//机器从1号计数

                          machineInfo->number = cell->secondOutDoorCount;
                          machineInfo->index = subMachineIndex;
                          break;
                        case MONITOR_TYPE:
                          break;
                        case GUARD_TYPE:
                          break;
                        case AREA_DOOR_TYPE:
                          break;
                        case MANAGER_TYPE:
                          break;
                        default :
                           printf("unknow device \n");
                          break;
                      }
                      break;

                    }
                  }


                }
              }
            }
          }

        }
      }
    }
    calculationIP(area,machineNumber,machineInfo->ipid,&machineInfo->machineIPInfo);
    return machineNumber;
}
