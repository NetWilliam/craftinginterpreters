#ifndef __TRIE__
#define __TRIE__

typedef struct TNode TNode;
struct TNode {
    int next[128];
};
void trie_init(int n);
void trie_free();
int trie_insert(const char *);
int trie_query(const char *);
#endif
