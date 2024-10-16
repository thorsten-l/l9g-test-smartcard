/*
 * Copyright 2024 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l9g.test.smartcard;

import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author th
 */
@Slf4j
public class L9gTestSmartcard
{
  public static void main( String[] args )
  {
    TerminalFactory factory = TerminalFactory.getDefault();
    CardTerminals terminals = factory.terminals();

    while( true )
    {
      try
      {
        if( terminals != null && !terminals.list().isEmpty() )
        {
          log.info( "Available card readers: {}", terminals.list() );

          CardTerminal terminal = terminals.list().get( 0 );
          log.info( "Using card reader: {}", terminal.getName() );

          log.debug( "Waiting for a card..." );
          
          // DO NOT USE BLOCKING I/O
          // You will not be able to check the card reader's presence 
          // by using terminal.waitForCardPresent(0).
          terminal.waitForCardPresent( 5000 );

          if( terminal.isCardPresent() )
          {
            Card card = terminal.connect( "*" );
            System.out.println( "Card: " + card );
            System.out.println( "Card Protocol: " + card.getProtocol() );
            byte[] cardAtr = card.getATR().getBytes();
            System.out.println( "Card ATR: " + bytesToHex( cardAtr ) );

            byte[] command = new byte[]
            {
              (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00
            };
            CommandAPDU commandAPDU = new CommandAPDU( command );
            ResponseAPDU response = card.getBasicChannel().transmit( commandAPDU );
            byte[] uidBytes = response.getData();
            System.out.println( "Card UID: " + bytesToHex( uidBytes ) );
            
            // In this format the card uid/serial is stored in our directory service
            System.out.println( "Card Serial: " + bytesToLongLittleEndian( uidBytes ) );

            // remove active sessions on card
            card.disconnect( true );
            
            do
            {
              log.debug( "Waiting for card removal..." );
              // DO NOT USE BLOCKING I/O
              terminal.waitForCardAbsent( 5000 );
            }
            while( terminal.isCardPresent() && !terminals.list().isEmpty() );
          }
        }
        else
        {
          log.error( "ERROR: No card reader found!" );
          Thread.sleep( 5000 );
        }
      }
      catch( Throwable t )
      {
        log.error( "ERROR: {}" + t.getMessage() );
      }
    }
  }

  private static String bytesToHex( byte[] bytes )
  {
    StringBuilder sb = new StringBuilder();
    for( byte b : bytes )
    {
      sb.append( String.format( "%02X", b ) );
    }
    return sb.toString();
  }

  public static long bytesToLongLittleEndian( byte[] bytes )
  {
    long uid = 0;
    for( int i = bytes.length - 1; i >= 0; i-- )
    {
      uid <<= 8;
      uid += ( 0x00ff & bytes[ i ] );
    }
    return uid;
  }

}

/*
Source:
https://gist.githubusercontent.com/hemantvallabh/d24d71a933e7319727cd3daa50ad9f2c/raw/3fdf02fb4601e6fe526c1f81afe767c6278adafc/APDUList.txt


Cheef's Grand APDU List Smartcard Selected Information APDU list
Reference: http://web.archive.org/web/20090630004017/http://cheef.ru/docs/HowTo/APDU.info

#------------+------------------------+------------------------+----------------------+--------------------------------+
|ClaIns P1 P2|Lc Send Data            |Le  Recv Data           | Specification        | Description                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    04                                                        | ISO 7816-9 6.3       | DEACTIVATE FILE                |
| A0 04 00 00 00                                               | 3GPP TS 11.11        | INVALIDATE                     |
| A0 04 00 00 00                                               | SAGEM SCT U34 6.15   | INVALIDATE                     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 0D xx xx 08 xxxx xxxx xxxx xxxx                           | SAGEM SCT U34        | VERIFY TRANSPORT CODE          |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    0C                                                        | ISO 7816-4 7.3.6     | ERASE RECORD (S)               |
| 80 0C 00 xx                          xx                      | SAGEM SCT U34 8.1.2  | CHECK (flash)                  |
| 80 0C 01 xx                          xx                      | SAGEM SCT U34 8.1.2  | CHECK (EEPROM)                 |
| 80 0C 02 xx                          xx                      | SAGEM SCT U34 8.1.2  | CHECK (checksum of file)       |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    0E                                                        | ISO 7816-4 8.2.4     | ERASE BINARY                   |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    10                                                        | ISO 7816-7           | PERFORM SCQL OPERATION         |
| 00 10 00 80 xx table name, ...                               | ISO 7816-7 7.1       | CREATE TABLE                   |
| 00 10 00 81 xx view name, table name                         | ISO 7816-7 7.2       | CREATE VIEW                    |
| 00 10 00 82 xx dictionary name                               | ISO 7816-7 7.3       | CREATE DICTIONARY              |
| 00 10 00 83 xx table name                                    | ISO 7816-7 7.4       | DROP TABLE                     |
| 00 10 00 84 xx view or dictionary                            | ISO 7816-7 7.5       | DROP VIEW                      |
| 00 10 00 85 xx privileges                                    | ISO 7816-7 7.6       | GRANT                          |
| 00 10 00 86 xx privileges                                    | ISO 7816-7 7.7       | REVOKE                         |
| 00 10 00 87 xx data                                          | ISO 7816-7 7.8       | DECLARE CURSOR                 |
| 00 10 00 88                                                  | ISO 7816-7 7.9       | OPEN                           |
| 00 10 00 89                                                  | ISO 7816-7 7.10      | NEXT                           |
| 00 10 00 8A                          xx D, fixing N (columns)| ISO 7816-7 7.11      | FETCH                          |
| 00 10 00 8B                          xx D, fixing N (columns)| ISO 7816-7 7.12      | FETCH NEXT                     |
| 00 10 00 8C xx data                                          | ISO 7816-7 7.13      | INSERT                         |
| 00 10 00 8D xx data                                          | ISO 7816-7 7.14      | UPDATE                         |
| 00 10 00 8E                                                  | ISO 7816-7 7.15      | DELETE                         |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    12                                                        | ISO 7816-7           | PERFORM TRANSACTION OPERATION  |
| 00 12 00 80                                                  | ISO 7816-7 8.2.1     | BEGIN                          |
| 00 12 00 81                                                  | ISO 7816-7 8.2.2     | COMMIT                         |
| 00 12 00 82                                                  | ISO 7816-7 8.2.3     | ROLLBACK                       |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    14                                                        | ISO 7816-7           | PERFORM USER OPERATION         |
| 00 14 00 80 xx User ID, ...                                  | ISO 7816-7 9.2.1     | PRESENT USER                   |
| 00 14 00 81 xx User ID, profile, ...                         | ISO 7816-7 9.2.2     | CREATE USER                    |
| 00 14 00 82 xx User ID                                       | ISO 7816-7 9.2.3     | DELETE USER                    |
| 80 14 xx xx 00                                               | GEMPLUS MPCOS-EMV    | Switch Protocol                |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 84 16 00 00 xx MAC                                           | VSDC                 | CARD BLOCK                     |
| 80 16 0X 00 05 xxxx xxxx xx                                  | GEMPLUS MPCOS-EMV    | Freeze Access Conditions       |
| 84 16 0X 00 08 xxxx xxxx xxxx xxxx                           | GEMPLUS MPCOS-EMV    | Freeze Access Conditions       |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 84 18 00 00 xx MAC                                           | VSDC                 | APPLICATION UNBLOCK            |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 84 1E 00 00 xx MAC                                           | VSDC                 | APPLICATION BLOCK              |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    20                                                        | ISO 7816-4 8.5.5     | VERIFY                         |
| 00 20 00 80 08 xxxx xxxx xxxx xxxx                           | VSDC                 | VERIFY (Transaction PIN data)  |
| A0 20 00 xx 08 CHV Value                                     | 3GPP TS 11.11        | VERIFY                         |
| A0 20 00 xx 08 CHV Value                                     | SAGEM SCT U34 6.10   | VERIFY                         |
| 80 20 00 xx 08 ADM Value                                     | SAGEM SCT U34 8.1.4  | VERIFY ADM                     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 21 00 xx 08 ADM Value                                     | SAGEM SCT U34 8.1.4  | VERIFY ADM                     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    22                                                        | ISO 7816-4 8.5.10    | MANAGE SECURITY ENVIRONMENT    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    24                                                        | ISO 7816-4 8.5.6     | CHANGE CHV                     |
| 84 24 00 00 xx PIN data + MAC                                | VSDC                 | PIN CHANGE/UNBLOCK             |
| A0 24 00 xx 10 Old CHV, New CHV                              | 3GPP TS 11.11        | CHANGE CHV                     |
| A0 24 00 xx 10 Old CHV, New CHV                              | SAGEM SCT U34 6.11   | CHANGE CHV                     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    26                                                        | ISO 7816-4 8.5.8     | DISABLE CHV1                   |
| A0 26 00 01 08 CHV1 value                                    | 3GPP TS 11.11        | DISABLE CHV1                   |
| A0 26 00 01 08 CHV1 value                                    | SAGEM SCT U32 6.12   | DISABLE CHV1                   |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    28                                                        | ISO 7816-4 8.5.7     | ENABLE CHV1                    |
| A0 28 00 01 08 CHV1 value                                    | 3GPP TS 11.11        | ENABLE CHV1                    |
| A0 28 00 01 08 CHV1 value                                    | SAGEM SCT U34 6.13   | ENABLE CHV1                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    2A                                                        | ISO 7816-8 5.2       | PERFORM SECURITY OPERATION     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    2C                                                        | ISO 7816-4 8.5.9     | UNBLOCK CHV                    |
| A0 2C 00 xx 10 Unblock CHV(PUK), New CHV                     | 3GPP TS 11.11        | UNBLOCK CHV                    |
| A0 2C 00 xx 10 Unblock CHV(PUK), New CHV                     | SAGEM SCT U34 6.14   | UNBLOCK CHV                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| A0 2E 00 0# 01 Data                                          | 3GPP TS 11.11        | WRITE CODE STATUS              |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| A0 32 00 00 03 Value to be added.                            | 3GPP TS 11.11        | INCREASE                       |
| A0 32 00 00 03 Value to be added.                            | SAGEM SCT U34 6.9    | INCREASE                       |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    39                                                        |                      | java Authentificate User Comman|
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    44                                                        | ISO 7816-9 6.4       | ACTIVATE FILE                  |
| A0 44 00 00 00                                               | 3GPP TS 11.11        | REHABILIDATE                   |
| A0 44 00 00 00                                               | SAGEM SCT U34 6.16   | REHABILIDATE                   |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    46                                                        | ISO 7816-8 5.1       | GENERATE ASYMMETRIC KEY PAIR   |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 50 xx xx 08 Host challenge        00                      | GlobalPlatform       | INITIALIZE UPDATE then [C0]    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    70                                                        | ISO 7816-4 8.1.2     | MANAGE CHANNEL                 |
| 00 70 xx xx                          xx                      | GlobalPlatform       | MANAGE CHANNEL                 |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 78 00 03 xx                                               | GlobalPlatform       | END R-MAC SESSION              |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 7A xx 01 xx Data and C-MAC, if needed                     | GlobalPlatform       | BEGIN R-MAC SESSION            |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    82                                                        | ISO 7816-4 8.5.3     | EXTERNAL AUTHENTICATE          |
| 84 82 00 00 10 Host cryptogram and MAC                       | GlobalPlatform       | EXTERNAL AUTHENTICATE          |
| 84 82 00 00 0A Authentication-related data                   | VSDC                 | EXTERNAL AUTHENTICATE          |
| 00 82 00 xx 06 Manual                                        | GEMPLUS MPCOS-EMV    | EXTERNAL AUTHENTICATE          |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    84                                                        | ISO 7816-4 8.5.2     | GET CHALLENGE                  |
| 00 84 00 00                          08 Rnd Num              | VSDC                 | GET CHALLENGE                  |
| 00 84 xx xx                          08 Rnd Num              | GEMPLUS MPCOS-EMV    | GET CHALLENGE                  |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    86                                                        | ISO 7816-4 8.5.4     | GENERAL AUTHENTICATE           |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    88                                                        | ISO 7816-4 8.5.1     | INTERNAL AUTHENTICATE          |
| 00 88 XX xx 0A Manual                                        | GEMPLUS MPCOS-EMV    | INTERNAL AUTHENTICATE          |
| A0 88 00 00 10 RAND : Rnd num        xx  SRES( 4B) , Kc (8B) | 3GPP TS 11.11        | RUN GSM ALGORITHM              |
| A0 88 00 00 10 RAND : Rnd num        xx  SRES( 4B) , Kc (8B) | SAGEM SCT U34 6.17   | RUN GSM ALGORITHM              |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    A0                                                        | ISO 7816-4 8.2.5     | SEARCH BINARY                  |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    A2                                                        | ISO 7816-4 8.3.5     | SEEK                           |
| A0 A2 00 xx xx Pattern               xx                      | 3GPP TS 11.11        | SEEK                           |
| A0 A2 00 xx xx Pattern               xx                      | SAGEM SCT U34 6.8    | SEEK                           |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    A4                                                        | ISO 7816-4 8.1.1     | SELECT                         |
| 00 A4 04 00 xx AID                   00                      | GlobalPlatform       | SELECT                         |
| 00 A4 00 xx xx File ID || Name       00  Manual              | VSDC                 | SELECT                         |
| A0 A4 00 00 02 File ID                                       | 3GPP TS 11.11        | SELECT                         |
| A0 A4 00 00 02 File ID                                       | SAGEM SCT U34 6.1    | SELECT                         |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 A8 00 00 00                       00                      | VSDC                 | GET PROCESSING OPTIONS         |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 AE 00 xx Transaction-related data                         | VSDC                 |                                |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    B0                                                        | ISO 7816-4 8.2.1     | READ BINARY                    |
| 00 B0 xx xx                          xx                      | GEMPLUS MPCOS-EMV    | READ BINARY                    |
| A0 B0 xx xx                          xx                      | 3GPP TS 11.11        | READ BINARY                    |
| A0 B0 xx xx                          xx                      | SAGEM SCT U34 6.4    | READ BINARY                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    B2                                                        | ISO 7816-4 8.3.1     | READ RECORD                    |
| 00 B2 xx                             00                      | VSDC                 | READ RECORD                    |
| A0 B2 xx xx                          xx                      | 3GPP TS 11.11        | READ RECORD                    |
| A0 B2 xx xx                          xx                      | SAGEM SCT U34 6.6    | READ RECORD                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    B4                                                        |                      | java Component Data            |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    B8                                                        |                      | java Create Applet             |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    BA                                                        |                      | java CAP end                   |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    BC                                                        |                      | java Component end             |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    BE                                04 Data                 | GEMPLUS GemClub-MEMO | READ                           |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    C0                                                        | ISO 7816-4 8.6.1     | GET RESPONSE                   |
| 00 C0                                1C Key Info             | GlobalPlatform       | GET RESPONSE                   |
| 00 C0 00 00                          00                      | VSDC                 | GET RESPONSE                   |
| 80 C0 00 00                          xx                      | GEMPLUS MPCOS-EMV    | Get Info on Get Response       |
| 80 C0 02 A0                          08 Chip SN              | GEMPLUS MPCOS-EMV    | Get Info                       |
| 80 C0 02 A1                          08 Card SN              | GEMPLUS MPCOS-EMV    | Get Info                       |
| 80 C0 02 A2                          08 Issuer SN            | GEMPLUS MPCOS-EMV    | Get Info                       |
| 80 C0 02 A3                          04 Iss.Ref.N            | GEMPLUS MPCOS-EMV    | Get Info                       |
| 80 C0 02 A4                          0D Chip Inf             | GEMPLUS MPCOS-EMV    | Get Info                       |
| 80 C0 02 A5                          xx Keys                 | GEMPLUS MPCOS-EMV    | Get Info                       |
| 80 C0 02 A6                          02 Last DF/EF           | GEMPLUS MPCOS-EMV    | Get Info                       |
| A0 C0 00 00                          xx                      | 3GPP TS 11.11        | GET RESPONSE                   |
| A0 C0 00 00                          xx                      | SAGEM SCT U34 6.3    | GET RESPONSE                   |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    C2                                                        | ISO 7816-4 8.6.2     | ENVELOPE                       |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    C4                                                        |                      | java Delete Applets            |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    CA                                                        | ISO 7816-4 8.4.1     | GET DATA                       |
| 00 CA 00 xx xx MAC, if present                               | GlobalPlatform       | GET DATA                       |
| 80 CA xx xx xx                                               | VSDC                 | GET DATA                       |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    D0                                                        | ISO 7816-4 8.2.2     | WRITE BINARY                   |
| 80 D0 xx xx xx Data to be written in EEPROM                  | VSDC                 | LOAD STRUCTURE                 |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    D2                                                        | ISO 7816-4 8.3.2     | WRITE RECORD                   |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    D6                                                        | ISO 7816-4 8.2.3     | UPDATE BINARY                  |
| A0 D6 xx xx xx Data to be written in EEPROM                  | 3GPP TS 11.11        | UPDATE BINARY                  |
| A0 D6 xx xx xx Data to be written in EEPROM                  | SAGEM SCT U34 6.5    | UPDATE BINARY                  |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 D8 xx xx xx KEY Date (and MAC)    00                      | GlobalPlatform       | PUT KEY                        |
|    D8                                                        | EMV                  | Set Card Status(personalization|
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    DA                                                        | ISO 7816-4 8.4.2     | PUT DATA                       |
| 00 DA xx xx xx Data                                          | VSDC                 | PUT DATA                       |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    DC                                                        | ISO 7816-4           | UPDATE RECORD                  |
| 00 DC xx xx xx Data (and MAC)                                | VSDC                 | UPDATE RECORD                  |
| A0 DC xx xx xx Data to be written in EEPROM                  | 3GPP TS 11.11        | UPDATE RECORD                  |
| A0 DC xx xx xx Data to be written in EEPROM                  | SAGEM SCT U34 6.7    | UPDATE RECORD                  |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    DE       04 Data                                          | GEMPLUS GemClub-MEMO | UPDATE                         |
| A0 DE 00 00 03 Data                                          | 3GPP TS 11.11        | LOAD AoC(SICAP)                |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    E0                                                        | ISO 7816-9 6.1       | CREATE FILE                    |
| 80 E0 02 00 0C Manual                                        | GEMPLUS MPCOS-EMV    | CREATE FILE                    |
| 80 E0 xx xx xx FCI length                                    | 3GPP TS 11.11        | CREATE FILE                    |
| 80 E0 xx xx xx FCI length                                    | SAGEM SCT U34        | CREATE FILE                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    E2                                                        | ISO 7816-4 8.3.4     | APPEND RECORD                  |
| 80 E2 00 00 xx Record (and MAC)                              | GlobalPlatform       | APPEND RECORD                  |
| 00 E2 00 00 xx Record                                        | VSDC                 | APPEND RECORD                  |
| 00 E2 00 00 xx Record                                        | GEMPLUS MPCOS-EMV    | APPEND RECORD                  |
| 00 E2 00 00 xx Record                                        | 3GPP TS 11.11        | APPEND RECORD                  |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    E4                                                        | ISO 7816-9 6.2       | DELETE FILE                    |
| 80 E4 00 00 xx TLV coded name                                | GlobalPlatform       | DELETE FILE                    |
| A0 E4 00 00 02 xx xx                                         | 3GPP TS 11.11        | DELETE FILE                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    E6                                                        | ISO 7816-9 6.5       | TERMINATE DF                   |
| 80 E6 xx 00 xx Manual                                        | GlobalPlatform       | INSTALL                        |
| A0 E6 xx xx 00                                               | 3GPP TS 11.11        | LOCK RECORD                    |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    E8                                                        | ISO 7816-9 6.6       | TERMINATE EF                   |
| 80 E8 00 00 xx Record                                        | GlobalPlatform       | LOAD                           |
| A0 E8 00 xx 10 Data                                          | 3GPP TS 11.11        | READ DIRECTORY                 |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 EA 00 00 xx Data                                          | 3GPP TS 11.11        | CREATE BINARY                  |
| 80 EA 00 00 xx Data                                          | SAGEM SCT U34        | CREATE BINARY                  |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 EE 00 xx 00                                               | VSDC                 | WRITE LOCK                     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 F0 xx xx xx AID of Application (and MAC)                  | GlobalPlatform       | SET STATUS                     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| A0 F2 00 00 xx                                               | 3GPP TS 11.11        | GET STATUS                     |
| A0 F2 00 00 xx                                               | SAGEM SCT U34 6.2    | GET STATUS                     |
| 80 F2 xx xx                                                  | GlobalPlatform       | GET STATUS                     |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 F8 xx xx                          xx                      | SAGEM SCT U34 8.1.1  | DIR                            |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| A0 FA 00 00 00                                               | 3GPP TS 11.11        | SLEEP                          |
| A0 FA 00 00 00                                               | SAGEM SCT U34 6.18   | SLEEP                          |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 FB xx xx                          xx                      | SAGEM SCT U34 8.1.1  | DIR                            |
+------------+------------------------+------------------------+----------------------+--------------------------------+
| 80 FC xx xx                          10                      | SAGEM SCT U34 8.1.3  | READ INFO                      |
+------------+------------------------+------------------------+----------------------+--------------------------------+
|    FE                                                        | ISO 7816-9 6.7       | TERMINATE CARD USAGE           |
| 80 FE xx xx 00                                               | SAGEM SCT U34        | BLOW FUSE                      |
+------------+------------------------+------------------------+----------------------+--------------------------------+
*/