package com.idevel.dailyinspection.utils;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Parcelable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class NfcUtils {
  public static String resolveIntent(Intent intent) {
    Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    NdefMessage[] msgs;

    if (rawMsgs != null) {
      msgs = new NdefMessage[rawMsgs.length];

      for (int i = 0; i < rawMsgs.length; i++) {
        msgs[i] = (NdefMessage) rawMsgs[i];
      }

      return buildTagViews(msgs);
    } else {
      byte[] empty = new byte[0];
      byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

      Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      byte[] payload = dumpTagData(tag).getBytes();
      NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
      NdefMessage msg = new NdefMessage(new NdefRecord[]{record});

      msgs = new NdefMessage[]{msg};

//        setReadTagDataa(msg);
      return buildTagViews(msgs);
    }
  }

  public static void setReadTagDataa(NdefMessage ndefmsg) {
    if (ndefmsg == null) {
      return;
    }

    String msgs = "";
    msgs += ndefmsg.toString() + "\n";
    NdefRecord[] records = ndefmsg.getRecords();

    for (NdefRecord rec : records) {
      byte[] payload = rec.getPayload();
      String textEncoding = "UTF-8";
      if (payload.length > 0)
        textEncoding = (payload[0] & 0200) == 0 ? "UTF-8" : "UTF-16";

      Short tnf = rec.getTnf();
      String type = String.valueOf(rec.getType());
      String payloadStr = new String(rec.getPayload(), Charset.forName(textEncoding));

      DLog.e("bjj showNFC :: aa "
          + msgs + " ^ "
          + tnf + " ^ "
          + type + " ^ "
          + payloadStr);
    }
  }

  public static String buildTagViews(NdefMessage[] msgs) {
    if (msgs == null || msgs.length == 0) return "";

    String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
    byte[] payload = msgs[0].getRecords()[0].getPayload();
    String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
    int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
    // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

    try {
      // Get the Text
      text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    } catch (UnsupportedEncodingException e) {
      Log.e("UnsupportedEncoding", e.toString());
    }

    DLog.e("bjj showNFC :: cc " + text);

    return text;
  }

  //sample 2
  public static String dumpTagData(Tag tag) {
    StringBuilder sb = new StringBuilder();
    byte[] id = tag.getId();
    sb.append("ID (hex): ").append(toHex(id)).append('\n');
    sb.append("ID (reversed hex):").append(toReversedHex(id)).append('\n');
    sb.append("ID (dec): ").append(toDec(id)).append('\n');
    sb.append("ID (reversed dec):").append(toReversedDec(id)).append('\n');
    String prefix = "android.nfc.tech.";
    sb.append("Technologies: ");
    for (String tech : tag.getTechList()) {
      sb.append(tech.substring(prefix.length()));
      sb.append(", ");
    }
    sb.delete(sb.length() - 2, sb.length());
    for (String tech : tag.getTechList()) {
      if (tech.equals(MifareClassic.class.getName())) {
        sb.append('\n');
        String type = "Unknown";
        try {
          MifareClassic mifareTag = MifareClassic.get(tag);
          switch (mifareTag.getType()) {
            case MifareClassic.TYPE_CLASSIC:
              type = "Classic";
              break;
            case MifareClassic.TYPE_PLUS:
              type = "Plus";
              break;
            case MifareClassic.TYPE_PRO:
              type = "Pro";
              break;
          }
          sb.append("Mifare Classic type: ");
          sb.append(type);
          sb.append('\n');
          sb.append("Mifare size: ");
          sb.append(mifareTag.getSize() + " bytes");
          sb.append('\n');
          sb.append("Mifare sectors: ");
          sb.append(mifareTag.getSectorCount());
          sb.append('\n');
          sb.append("Mifare blocks: ");
          sb.append(mifareTag.getBlockCount());
        } catch (Exception e) {
          sb.append("Mifare classic error: " + e.getMessage());
        }
      }
      if (tech.equals(MifareUltralight.class.getName())) {
        sb.append('\n');
        MifareUltralight mifareUlTag = MifareUltralight.get(tag);
        String type = "Unknown";
        switch (mifareUlTag.getType()) {
          case MifareUltralight.TYPE_ULTRALIGHT:
            type = "Ultralight";
            break;
          case MifareUltralight.TYPE_ULTRALIGHT_C:
            type = "Ultralight C";
            break;
        }
        sb.append("Mifare Ultralight type: ");
        sb.append(type);
      }
    }
    return sb.toString();
  }

  public static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (int i = bytes.length - 1; i >= 0; --i) {
      int b = bytes[i] & 0xff;
      if (b < 0x10)
        sb.append('0');
      sb.append(Integer.toHexString(b));
      if (i > 0) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  public static String toReversedHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; ++i) {
      if (i > 0) {
        sb.append(" ");
      }
      int b = bytes[i] & 0xff;
      if (b < 0x10)
        sb.append('0');
      sb.append(Integer.toHexString(b));
    }
    return sb.toString();
  }

  public static long toDec(byte[] bytes) {
    long result = 0;
    long factor = 1;
    for (int i = 0; i < bytes.length; ++i) {
      long value = bytes[i] & 0xffl;
      result += value * factor;
      factor *= 256l;
    }
    return result;
  }

  public static long toReversedDec(byte[] bytes) {
    long result = 0;
    long factor = 1;
    for (int i = bytes.length - 1; i >= 0; --i) {
      long value = bytes[i] & 0xffl;
      result += value * factor;
      factor *= 256l;
    }
    return result;
  }


  //sample 4
//  public static String detectTagData(Tag tag) {
//    StringBuilder sb = new StringBuilder();
//    byte[] id = tag.getId();
//    sb.append("ID (hex): ").append(toHex(id)).append('\n');
//    sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');
//    sb.append("ID (dec): ").append(toDec(id)).append('\n');
//    sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n');
//
//    String prefix = "android.nfc.tech.";
//    sb.append("Technologies: ");
//    for (String tech : tag.getTechList()) {
//      sb.append(tech.substring(prefix.length()));
//      sb.append(", ");
//    }
//
//    sb.delete(sb.length() - 2, sb.length());
//
//    for (String tech : tag.getTechList()) {
//      if (tech.equals(MifareClassic.class.getName())) {
//        sb.append('\n');
//        String type = "Unknown";
//
//        try {
//          MifareClassic mifareTag = MifareClassic.get(tag);
//
//          switch (mifareTag.getType()) {
//            case MifareClassic.TYPE_CLASSIC:
//              type = "Classic";
//              break;
//            case MifareClassic.TYPE_PLUS:
//              type = "Plus";
//              break;
//            case MifareClassic.TYPE_PRO:
//              type = "Pro";
//              break;
//          }
//          sb.append("Mifare Classic type: ");
//          sb.append(type);
//          sb.append('\n');
//
//          sb.append("Mifare size: ");
//          sb.append(mifareTag.getSize() + " bytes");
//          sb.append('\n');
//
//          sb.append("Mifare sectors: ");
//          sb.append(mifareTag.getSectorCount());
//          sb.append('\n');
//
//          sb.append("Mifare blocks: ");
//          sb.append(mifareTag.getBlockCount());
//        } catch (Exception e) {
//          sb.append("Mifare classic error: " + e.getMessage());
//        }
//      }
//
//      if (tech.equals(MifareUltralight.class.getName())) {
//        sb.append('\n');
//        MifareUltralight mifareUlTag = MifareUltralight.get(tag);
//        String type = "Unknown";
//        switch (mifareUlTag.getType()) {
//          case MifareUltralight.TYPE_ULTRALIGHT:
//            type = "Ultralight";
//            break;
//          case MifareUltralight.TYPE_ULTRALIGHT_C:
//            type = "Ultralight C";
//            break;
//        }
//        sb.append("Mifare Ultralight type: ");
//        sb.append(type);
//      }
//    }
//    Log.v("test", sb.toString());
//    return sb.toString();
//  }
//
//  public static String toHex(byte[] bytes) {
//    StringBuilder sb = new StringBuilder();
//    for (int i = bytes.length - 1; i >= 0; --i) {
//      int b = bytes[i] & 0xff;
//      if (b < 0x10)
//        sb.append('0');
//      sb.append(Integer.toHexString(b));
//      if (i > 0) {
//        sb.append(" ");
//      }
//    }
//    return sb.toString();
//  }
//
//  public static String toReversedHex(byte[] bytes) {
//    StringBuilder sb = new StringBuilder();
//    for (int i = 0; i < bytes.length; ++i) {
//      if (i > 0) {
//        sb.append(" ");
//      }
//      int b = bytes[i] & 0xff;
//      if (b < 0x10)
//        sb.append('0');
//      sb.append(Integer.toHexString(b));
//    }
//    return sb.toString();
//  }
//
//  public static long toDec(byte[] bytes) {
//    long result = 0;
//    long factor = 1;
//    for (int i = 0; i < bytes.length; ++i) {
//      long value = bytes[i] & 0xffl;
//      result += value * factor;
//      factor *= 256l;
//    }
//    return result;
//  }
//
//  public static long toReversedDec(byte[] bytes) {
//    long result = 0;
//    long factor = 1;
//    for (int i = bytes.length - 1; i >= 0; --i) {
//      long value = bytes[i] & 0xffl;
//      result += value * factor;
//      factor *= 256l;
//    }
//    return result;
//  }

  //sample 5
//  private static final byte FLAG_MB = (byte) 0x80;
//  private static final byte FLAG_ME = (byte) 0x40;
//  private static final byte FLAG_CF = (byte) 0x20;
//  private static final byte FLAG_SR = (byte) 0x10;
//  private static final byte FLAG_IL = (byte) 0x08;
//
//  public static void onCreateaaaa(Intent intent) {
//    String intentAction = intent.getAction();
//
//    // TAG검출 시의 인텐트인지 여부 점검
//    if (intentAction.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
//      DLog.i("bjj NFC02" + NfcAdapter.ACTION_TAG_DISCOVERED);
//
//      // NFC에서 취득한 메시지 취득
//      Parcelable[] msgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//
//      // NdefMessage형 배열 선언
//      NdefMessage[] nmsgs = new NdefMessage[msgs.length];
//
//      for (int i = 0; i < msgs.length; i++) {
//
//        // Parcelable을NdefMessage에 캐스트
//        nmsgs[i] = (NdefMessage) msgs[i];
//
//        // 태그 유형을 점검
//        NFCType type = getTagType(nmsgs[i]);
//        if (type == NFCType.TEXT) {
//          DLog.i("NFCType", "bjj NFCType.TEXT");
//        } else if (type == NFCType.URI) {
//          DLog.i("NFCType", "bjj NFCType.URI");
//        } else if (type == NFCType.SMART_POSTER) {
//          DLog.i("NFCType", "bjj NFCType.SMART_POSTER");
//        } else if (type == NFCType.ABSOLUTE_URI) {
//          DLog.i("NFCType", "bjj NFCType.ABSOLUTE_URI");
//        }
//
//        // 메시지 레코드를 NdefMessage로 취득
//        byte[] bytes = getTagData(nmsgs[i]);
//
//        // NdefRecord의 첫 번째 1바이트는 헤드
//        int header = bytes[0];
//
//        // MB(Message Begin)플래그 점검
//        if ((header & FLAG_MB) > 0) {
//          DLog.i("TagHeader", "bjj FLAG_MB = on");
//        } else {
//          DLog.i("TagHeader", "bjj FLAG_MB = off");
//        }
//
//        // MB(Message End) 플래그 점검
//        if ((header & FLAG_ME) > 0) {
//          DLog.i("TagHeader", "bjj FLAG_ME = on");
//        } else {
//          DLog.i("TagHeader", "bjj FLAG_ME = off");
//        }
//
//        // CF(Chunk Flag)의 점검
//        if ((header & FLAG_CF) > 0) {
//          DLog.i("TagHeader", "bjj FLAG_CF = on");
//        } else {
//          DLog.i("TagHeader", "bjj FLAG_CF = off");
//        }
//
//        // SR(Short Record) 플래그 점검
//        if ((header & FLAG_SR) > 0) {
//          DLog.i("TagHeader", "bjj FLAG_SR = on");
//        } else {
//          DLog.i("TagHeader", "bjj FLAG_SR = off");
//        }
//
//        // IL(ID Length) 플래그 점검
//        if ((header & FLAG_IL) > 0) {
//          DLog.i("TagHeader", "bjj FLAG_IL = on");
//        } else {
//          DLog.i("TagHeader", "bjj FLAG_IL = off");
//        }
//
//        // TNF(Type Name Format Type)의 점검
//        if ((header & NdefRecord.TNF_EMPTY) > 0) {
//          DLog.i("TagHeader", "bjj TNF = NdefRecord.TNF_EMPTY");
//        } else if ((header & NdefRecord.TNF_WELL_KNOWN) > 0) {
//          DLog.i("TagHeader", "bjj TNF = NdefRecord.TNF_WELL_KNOWN");
//        } else if ((header & NdefRecord.TNF_MIME_MEDIA) > 0) {
//          DLog.i("TagHeader", "bjj TNF = NdefRecord.TNF_MIME_MEDIA");
//        } else if ((header & NdefRecord.TNF_EXTERNAL_TYPE) > 0) {
//          DLog.i("TagHeader", "bjj TNF = NdefRecord.TNF_EXTERNAL_TYPE");
//        } else if ((header & NdefRecord.TNF_EXTERNAL_TYPE) > 0) {
//          DLog.i("TagHeader", "bjj TNF = NdefRecord.TNF_EXTERNAL_TYPE");
//        } else if ((header & NdefRecord.TNF_UNKNOWN) > 0) {
//          DLog.i("TagHeader", "bjj TNF = NdefRecord.TNF_UNKNOWN");
//        } else if ((header & NdefRecord.TNF_UNCHANGED) > 0) {
//          DLog.i("TagHeader", "bjj TNF = NdefRecord.TNF_UNCHANGED");
//        }
//
//        // NdefRecord의 두 번째 바이트 이후는 Tag 본문
//        if (type == NFCType.TEXT) {
//          String lang = new String(new byte[]{bytes[1], bytes[2],});
//          DLog.i("bjj Lang: " + lang);
//          for (int j = 3; j < bytes.length; j++) {
//            String string = new String(new byte[]{bytes[j]});
//            DLog.i("bjj TagBody"+string);
//          }
//        } else if (type == NFCType.SMART_POSTER) {
//          // NdefRecord의 두 번째 바이트는 Record Type의 길이
//          byte recordtype = bytes[1];
//          DLog.i("bjj Record Type length = " + recordtype);
//
//          // NdefRecord의 세 번째 바이트는 Preload의 길이
//          byte prelength = bytes[2];
//          DLog.i("bjj Preload length = " + prelength);
//
//          // NdefRecord의 네 번째 바이트는 레코드 명
//          byte recordname = bytes[3];
//          String RecordName = new String(new byte[]{recordname});
//          DLog.i("bjj Record Name = " + RecordName);
//
//          if (RecordName.equalsIgnoreCase("U")) {
//            // NdefRecord의 다섯 번째 바이트는 URI ID
//            byte uriID = bytes[4];
//            DLog.i("bjj URI ID = " + getMap(uriID));
//
//            for (int j = 5; j < bytes.length; j++) {
//              DLog.i("bjj TagBody"+new String(new byte[]{bytes[j]}));
//            }
//          }
//
//        }
//      }
//    }
//  }

//  // enum의 정의
//  private static enum NFCType {
//    UNKNOWN, TEXT, URI, SMART_POSTER, ABSOLUTE_URI
//  }
//
//  // 태그 유형을 점검하는 메소드
//  public static NFCType getTagType(final NdefMessage msg) {
//    if (msg == null) return null;
//
//    for (NdefRecord record : msg.getRecords()) {
//
//      // TNF(Type Name Format)을 점검
//      if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
//
//        // RTD(Record Type Definition)을 점검
//        if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
//          return NFCType.TEXT;
//        } else if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
//          return NFCType.URI;
//        } else if (Arrays.equals(record.getType(), NdefRecord.RTD_SMART_POSTER)) {
//          return NFCType.SMART_POSTER;
//        }
//      } else if (record.getTnf() == NdefRecord.TNF_ABSOLUTE_URI) {
//        return NFCType.ABSOLUTE_URI;
//      }
//    }
//    return null;
//  }
//
//  // 메시지 레코드를 NdefMessage에서 취득
//  public static byte[] getTagData(final NdefMessage msg) {
//    if (msg == null) return null;
//
//    for (NdefRecord record : msg.getRecords()) {
//      byte[] payload = record.getPayload();
//      if (payload != null) return payload;
//    }
//    return null;
//  }
//
//  public static String getMap(byte bt) {
//    if (bt == (byte) 0x00) {
//      return "";
//    } else if (bt == (byte) 0x01) {
//      return "http://www.";
//    } else if (bt == (byte) 0x02) {
//      return "https://www.";
//    } else if (bt == (byte) 0x03) {
//      return "http://";
//    } else if (bt == (byte) 0x04) {
//      return "https://";
//    } else if (bt == (byte) 0x05) {
//      return "tel:";
//    } else if (bt == (byte) 0x06) {
//      return "mailto:";
//    } else if (bt == (byte) 0x07) {
//      return "ftp://anonymous:anonymous@";
//    } else if (bt == (byte) 0x08) {
//      return "ftp://ftp.";
//    } else if (bt == (byte) 0x09) {
//      return "ftps://";
//    } else if (bt == (byte) 0x0A) {
//      return "sftp://";
//    } else if (bt == (byte) 0x0B) {
//      return "smb://";
//    } else if (bt == (byte) 0x0C) {
//      return "nfs://";
//    } else if (bt == (byte) 0x0D) {
//      return "ftp://";
//    } else if (bt == (byte) 0x0E) {
//      return "dav://";
//    } else if (bt == (byte) 0x0F) {
//      return "news:";
//    } else if (bt == (byte) 0x10) {
//      return "telnet://";
//    } else if (bt == (byte) 0x11) {
//      return "imap:";
//    } else if (bt == (byte) 0x12) {
//      return "rtsp://";
//    } else if (bt == (byte) 0x13) {
//      return "urn:";
//    } else if (bt == (byte) 0x14) {
//      return "pop:";
//    } else if (bt == (byte) 0x15) {
//      return "sip:";
//    } else if (bt == (byte) 0x16) {
//      return "sips:";
//    } else if (bt == (byte) 0x17) {
//      return "tftp:";
//    } else if (bt == (byte) 0x18) {
//      return "btspp://";
//    } else if (bt == (byte) 0x19) {
//      return "btl2cap://";
//    } else if (bt == (byte) 0x1A) {
//      return "btgoep://";
//    } else if (bt == (byte) 0x1B) {
//      return "tcpobex://";
//    } else if (bt == (byte) 0x1C) {
//      return "irdaobex://";
//    } else if (bt == (byte) 0x1D) {
//      return "file://";
//    } else if (bt == (byte) 0x1E) {
//      return "urn:epc:id:";
//    } else if (bt == (byte) 0x1F) {
//      return "urn:epc:tag:";
//    } else if (bt == (byte) 0x20) {
//      return "urn:epc:pat:";
//    } else if (bt == (byte) 0x21) {
//      return "urn:epc:raw:";
//    } else if (bt == (byte) 0x22) {
//      return "urn:epc:";
//    } else if (bt == (byte) 0x23) {
//      return "urn:nfc:";
//    }
//    return null;
//  }

}
