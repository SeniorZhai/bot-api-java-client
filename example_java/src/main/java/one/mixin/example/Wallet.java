package one.mixin.example;

import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;
import one.mixin.lib.HttpClient;
import one.mixin.lib.SessionUtil;
import one.mixin.lib.api.MixinResponse;
import one.mixin.lib.util.Base64;
import one.mixin.lib.util.CryptoUtils;
import one.mixin.lib.vo.AccountRequest;
import one.mixin.lib.vo.Asset;
import one.mixin.lib.vo.PinRequest;
import one.mixin.lib.vo.User;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Wallet {
  private static class SecretKey {
    public static String USER_ID = "int put your bot user id";
    public static String PIN_TOKEN = "int put your bot pin token";
    public static String PIN = "int put your bot pin";
    public static String SESSION_ID = "int put your bot session id";
    public static String PRIVATE_KEY = "int put your bot private key";
  }

  private static HttpClient client =
      new HttpClient(SecretKey.USER_ID, SecretKey.SESSION_ID, SecretKey.PRIVATE_KEY);

  public static void main(String[] args) {
    Security.addProvider(new BouncyCastleProvider());
    KeyPair sessionKey = CryptoUtils.generateRSAKeyPair(1024);
    // if repeated use
    // client.clearUserInfo();
    User user = createUser(sessionKey);
    if (user == null) {
      System.out.println("Create user failure");
      return;
    } else {
      String privateKeyPem = CryptoUtils.getPrivateKeyPem(sessionKey);
      client.setUserInfo(
          new HttpClient.TokenInfo(user.getUserId(), user.getSessionId(), privateKeyPem));
      System.out.println("Create user success");
    }
    String userAesKey =
        CryptoUtils.rsaDecrypt(sessionKey.getPrivate(), user.getSessionId(), user.getPinToken());
    User result = setupPin(userAesKey, "131499");
    if (result == null) {
      System.out.println("Setup Pin failure");
    } else {
      System.out.println("Setup Pin success");
    }
    Asset asset = getAsset("965e5c6e-434c-3fa9-b780-c50f43cd955c");
    if (asset == null) {
      System.out.println("Get Asset failure");
    } else {
      System.out.println("Get Asset success");
    }
  }

  public static User createUser(KeyPair sessionKey) {
    String sessionSecret = Base64.encodeBytes(CryptoUtils.getPublicKey(sessionKey));
    try {
      MixinResponse<User> response = client.getUserService()
          .createUsers(new AccountRequest("User${Random().nextInt(100)}", sessionSecret))
          .execute()
          .body();
      if (response.isSuccess()) {
        return response.getData();
      } else {
        return null;
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return null;
    }
  }

  private static UserIterator userIterator;

  public static User setupPin(String userAesKey, String pin) {
    try {
      userIterator = new UserIterator();
      MixinResponse<User> response = client.getUserService()
          .createPin(new PinRequest(SessionUtil.encryptPin(userIterator, userAesKey, pin), ""))
          .execute()
          .body();
      if (response.isSuccess()) {
        return response.getData();
      } else {
        return null;
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return null;
    }
  }

  public static Asset getAsset(String assetId) {
    try {
      MixinResponse<Asset> response = client.getAssetService().getDeposit(assetId).execute().body();
      if (response.isSuccess()) {
        return response.getData();
      } else {
        return null;
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return null;
    }
  }
}
