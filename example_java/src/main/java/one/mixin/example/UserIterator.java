package one.mixin.example;

import one.mixin.lib.PinIterator;

public class UserIterator implements PinIterator {
  public long value = 1;

  @Override public long getValue() {
    return value;
  }

  @Override public void increment() {
    value++;
  }
}
