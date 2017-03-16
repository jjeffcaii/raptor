package as.leap.raptor.core.utils;

public final class Bytes {

  private Bytes() {
  }

  public static int toUInt8(final byte b) {
    return b & 0xFF;
  }

  public static int toUInt8R(final byte b, final int moveRight) {
    return (b & 0xFF) >>> moveRight;
  }

  public static int toUInt8L(final byte b, final int moveLeft) {
    return (b << moveLeft) & 0xFF;
  }

  public static void main(String[] args) {
    System.out.println(toUInt8R((byte) -61, 6));
  }

}
