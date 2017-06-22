package com.btk5h.skriptmirror;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
  private int cacheSize;

  public LRUCache(int cacheSize) {
    super(16, 0.75F, true);
    this.cacheSize = cacheSize;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() >= cacheSize;
  }
}
