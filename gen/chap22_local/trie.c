#include <stdlib.h>
#include "trie.h"

TNode *arr = NULL;
int *endpoint = NULL;
int end_cnt;
int node_cnt;

void trie_init(int n)
{
    arr = (TNode *) (malloc(sizeof(TNode) * n));
    endpoint = (int *) (malloc(sizeof(int) * n));
    node_cnt = 0;
    end_cnt = 0;
}
void trie_free()
{
    free(arr);
    free(endpoint);
}
int trie_insert(const char *s)
{
    int u = 0;
    for (int i = 0; s[i]; ++i) {
        if (arr[u].next[s[i]] == 0) {
            arr[u].next[s[i]] = ++node_cnt;
        }
        u = arr[u].next[s[i]];
    }
    if (endpoint[u] == 0)
        endpoint[u] = ++end_cnt;
    return endpoint[u];
}
int trie_query(const char *s)
{
    int u = 0;
    for (int i = 0; s[i]; ++i) {
        if (arr[u].next[s[i]] == 0)
            return 0;
        u = arr[u].next[s[i]];
    }
    return u;
}
