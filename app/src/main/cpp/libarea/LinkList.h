/*
 * LinkList.h
 *
 *  Created on: Feb 11, 2014
 *      Author: root
 */

#ifndef LINKLIST_H_
#define LINKLIST_H_


typedef void LinkList;
typedef struct _tag_LinkListNode LinkListNode;

struct _tag_LinkListNode
{
	LinkListNode * next;
};
#define offset(TYPE, MEMBER) ((size_t) &((TYPE *)0)->MEMBER)
#define container_of(ptr, type, member) ({          \
	const typeof(((type *)0)->member)*__mptr = (ptr);    \
		     (type *)((char *)__mptr - offset(type, member)); })

LinkList *linklist_create();

void linklist_clear(LinkList *list);

void linklist_destroy(LinkList *list);

int linklist_length(LinkList *list);

int linklist_insert(LinkList *list,int pos,LinkListNode *node);

LinkListNode *linklist_get(LinkList *list,int pos);

LinkListNode *linklist_deleate(LinkList *list,int pos);

#endif /* LINKLIST_H_ */
