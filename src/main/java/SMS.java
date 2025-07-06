import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

//This class sends text notifications given an Account SID, an Auth Token, a "from" Phone Number, and a "to" Phone Number.
public class SMS {
    public String accountId;
    public String authToken;
    public String fromPhoneNum;
    public String toPhoneNum;

    //Creates an SMS object that can send texts.
    public SMS(String account, String auth, String from, String to){
        accountId = account;
        authToken = auth;
        fromPhoneNum = from;
        toPhoneNum = to;
        Twilio.init(accountId, authToken);
    }

    //Sends a text.
    public void sendText(String message) {
        Message text = Message.creator(new PhoneNumber(toPhoneNum), new PhoneNumber(fromPhoneNum), message).create();
    }
}
