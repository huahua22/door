/*
 * LinkList.c
 *
 *  Created on: Feb 11, 2014
 *      Author: root
 */
#include <stdio.h>
#include <malloc.h>
#include "LinkList.h"

typedef struct _tag_TLinkList TLinkList;

struct _tag_TLinkList
{
	LinkListNode header;
	int length;
};
LinkList *linklist_create()
{
	TLinkList *ret = NULL;
	ret = (TLinkList *)malloc(sizeof(TLinkList));

	if(ret !=  NULL)
	{
		ret->length = 0;
		ret->header.next = NULL;
	}

	return ret;
}

void linklist_clear(LinkList *list)
{
	TLinkList *tlist = (TLinkList *)list;
	if(tlist != NULL)
	{
		tlist->length = 0;
		tlist->header.next = NULL;
	}
}

void linklist_destroy(LinkList *list)
{
	TLinkList *tlist = (TLinkList *)list;
	if(tlist != NULL)
	{
		free(list);
	}
}

int linklist_length(LinkList *list)
{
	TLinkList *tlist = (TLinkList *)list;
	if(tlist != NULL)
	{
		return tlist->length;
	}
	return -1;
}

int linklist_insert(LinkList *list,int pos,LinkListNode *node)
{
	TLinkList *tlist = (TLinkList *)list;
	int ret = (tlist != NULL) && (pos >= 0) && (node != NULL);
	if(ret)
	{
		LinkListNode *current = (LinkListNode *)tlist;
		int i;
		if(pos >= tlist->length)
			pos = tlist->length;
		for(i = 0;i < pos && current->next != NULL;i++)
		{
			current = current->next;
		}

		node->next = current->next;
		current->next = node;
		tlist->length++;
	}
	return ret;
}

LinkListNode *linklist_get(LinkList *list,int pos)
{
	TLinkList *tlist = (TLinkList *)list;
	LinkListNode *ret = NULL;

	if(tlist != NULL && pos >= 0 && pos <= tlist->length)
	{
		int i = 0;
		LinkListNode *current = (LinkListNode *)tlist;

		for(i = 0;i < pos && current->next != NULL;i++)
		{
			current = current->next;
		}

		ret = current->next;
	}

	return ret;
}

LinkListNode *linklist_deleate(LinkList *list,int pos)
{

	TLinkList *tlist = (TLinkList *)list;
	LinkListNode *ret = NULL;

	if(tlist != NULL && pos >= 0 && pos <= tlist->length)
	{
		int i = 0;
		LinkListNode *current = (LinkListNode *)tlist;

		for(i = 0;i < pos && current->next != NULL;i++)
		{
			current = current->next;
		}

		ret = current->next;
		current->next = ret->next;
		tlist->length--;
	}

	return ret;
}

