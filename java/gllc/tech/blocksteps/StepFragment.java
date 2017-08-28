package gllc.tech.blocksteps;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by bhangoo on 8/27/2017.
 */

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.parity.Parity;
import org.web3j.protocol.parity.ParityFactory;
import org.web3j.protocol.parity.methods.response.PersonalUnlockAccount;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import gllc.tech.blocksteps.Auomation.DateFormatter;
import gllc.tech.blocksteps.Auomation.SetAlarm;
import gllc.tech.blocksteps.Auomation._Users_Admin_Desktop_Steps_sol_Steps;
import gllc.tech.blocksteps.Services.StepService;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

public class StepFragment extends Fragment {

    ArrayList<Integer> peopleCount = new ArrayList<>();
    ProgressDialog startingDialog, creatingAccount, loadingAccount, settingUp;

    boolean firstLoad = false;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    TextView todayStepsBig;
    TextView day0Steps, day1Steps,day2Steps,day3Steps, day4Steps, day5Steps, day6Steps;
    TextView day0Title, day1Title, day2Title, day3Title, day4Title, day5Title, day6Title;
    TextView peopleCount0, peopleCount1, peopleCount2, peopleCount3, peopleCount4, peopleCount5, peopleCount6;
    TextView avgPeopleSteps0, avgPeopleSteps1 , avgPeopleSteps2, avgPeopleSteps3, avgPeopleSteps4, avgPeopleSteps5, avgPeopleSteps6;
    TextView versionName;

    FirebaseDatabase database = FirebaseDatabase.getInstance();

    Credentials credentials = null;
    Web3j web3;
    public static _Users_Admin_Desktop_Steps_sol_Steps contract;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.step_fragment, container, false);

        setup();
        view = assignUI(view);
        checkForWallet();

        try {
            unlockMainAccount();
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.i("--All", "Error: " + e.getMessage());
            Crashlytics.logException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i("--All", "Error: " + e.getMessage());
            Crashlytics.logException(e);
        }


        return view;
    }


    private void checkForWallet() {
        //editor.putString("walletFileName", "none").commit();
        if (sharedPref.getString("walletFileName","none").equals("none")){
            startingDialog.dismiss();
            creatingAccount = new ProgressDialog(getContext());
            creatingAccount.setMessage("First Time Loading - Setting Up");
            creatingAccount.show();

            firstLoad=true;
            new CreateWallet().execute();
        }

        else{ new LoadCredentials().execute();}
/*
        try {
            EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, MyApplication.contractAddress);
            web3.ethLogObservable(filter).subscribe(new Action1<org.web3j.protocol.core.methods.response.Log>() {
                @Override
                public void call(org.web3j.protocol.core.methods.response.Log log) {
                    Log.i("--All", "FIIIIIIIIIIIIIIIIIINDMEEEE" + log.getTransactionHash());
                }
            });
        } catch (Exception e) {
            Log.i("--All", "Error: " + e.getMessage());
        }
        */
/*
        Subscription subscription = web3.blockObservable(false).subscribe();


        Subscription subscription = web3.blockObservable(false).subscribe(block -> {
            Log.i("--All", "FIIIIIIIIIIIIIIIIIINDMEEEE" + "Sweet, block number " + block.getBlock().getNumber() + " has just been created");
        }, Throwable::printStackTrace);
        try {
            TimeUnit.MINUTES.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        subscription.unsubscribe();
*/
    }

    public void setup() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        editor = sharedPref.edit();

        startingDialog = new ProgressDialog(getContext());
        startingDialog.setMessage("Loading From BlockChain - Can Take A While");
        startingDialog.show();

        try {
            web3 = Web3jFactory.build(new HttpService("http://45.55.4.74:8545"));  // defaults to http://localhost:8545/
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("--All", "Exception Building WebFactory " + e.getMessage());
            Crashlytics.logException(e);
        }
    }

    public View assignUI(View view) {
        todayStepsBig = (TextView)view.findViewById(R.id.todayStepsLarge);

        avgPeopleSteps0 = (TextView)view.findViewById(R.id.avgPeopleSteps0);
        avgPeopleSteps1 = (TextView)view.findViewById(R.id.avgPeopleSteps1);
        avgPeopleSteps2 = (TextView)view.findViewById(R.id.avgPeopleSteps2);
        avgPeopleSteps3 = (TextView)view.findViewById(R.id.avgPeopleSteps3);
        avgPeopleSteps4 = (TextView)view.findViewById(R.id.avgPeopleSteps4);
        avgPeopleSteps5 = (TextView)view.findViewById(R.id.avgPeopleSteps5);
        avgPeopleSteps6 = (TextView)view.findViewById(R.id.avgPeopleSteps6);

        day0Steps = (TextView)view.findViewById(R.id.weekSteps0);
        day1Steps = (TextView)view.findViewById(R.id.weekSteps1);
        day2Steps = (TextView)view.findViewById(R.id.weekSteps2);
        day3Steps = (TextView)view.findViewById(R.id.weekSteps3);
        day4Steps = (TextView)view.findViewById(R.id.weekSteps4);
        day5Steps = (TextView)view.findViewById(R.id.weekSteps5);
        day6Steps = (TextView)view.findViewById(R.id.weekSteps6);

        day0Title = (TextView)view.findViewById(R.id.weekDayTitle0);
        day1Title = (TextView)view.findViewById(R.id.weekDayTitle1);
        day2Title = (TextView)view.findViewById(R.id.weekDayTitle2);
        day3Title = (TextView)view.findViewById(R.id.weekDayTitle3);
        day4Title = (TextView)view.findViewById(R.id.weekDayTitle4);
        day5Title = (TextView)view.findViewById(R.id.weekDayTitle5);
        day6Title = (TextView)view.findViewById(R.id.weekDayTitle6);

        peopleCount0 = (TextView)view.findViewById(R.id.peopleCount0);
        peopleCount1 = (TextView)view.findViewById(R.id.peopleCount1);
        peopleCount2 = (TextView)view.findViewById(R.id.peopleCount2);
        peopleCount3 = (TextView)view.findViewById(R.id.peopleCount3);
        peopleCount4 = (TextView)view.findViewById(R.id.peopleCount4);
        peopleCount5 = (TextView)view.findViewById(R.id.peopleCount5);
        peopleCount6 = (TextView)view.findViewById(R.id.peopleCount6);

        versionName = (TextView)view.findViewById(R.id.versionName);
        versionName.setText("Version: " + BuildConfig.VERSION_NAME);

        return view;
    }

    private class CreateWallet extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Log.i("--All", "Creating Wallet");
            DatabaseReference myRef = database.getReference(sharedPref.getString("uniqueId","NA"));
            myRef.child("First Load").push().setValue("Creating Wallet");

            //Creating wallet
            String fileName = "not set";
            try {
                fileName = WalletUtils.generateNewWalletFile(
                        sharedPref.getString("uniqueId","NA"),
                        new File(getActivity().getFilesDir(), "/"),false);
            } catch (CipherException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (IOException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            }

            Log.i("--All", "Filename: " + fileName);
            editor.putString("walletFileName",fileName).commit();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            creatingAccount.dismiss();
            loadingAccount = new ProgressDialog(getContext());
            loadingAccount.setMessage("Loading");
            loadingAccount.show();

            Log.i("--All", "Loading Wallet (from Wallet Creation)");
            DatabaseReference myRef = database.getReference(sharedPref.getString("uniqueId","NA"));
            myRef.child("First Load").push().setValue("Loading Wallet (from Wallet Creation)");

            new LoadCredentials().execute();
        }
    }

    private class LoadCredentials extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                credentials = WalletUtils.loadCredentials(
                        sharedPref.getString("uniqueId","NA"),
                        getActivity().getFilesDir().getAbsolutePath() + "/" + sharedPref.getString("walletFileName","none"));

                Log.i("--All", "Credentials Address: " + credentials.getAddress());

                editor.putString("ethAddress",credentials.getAddress()).commit();

                DatabaseReference myRef = database.getReference(sharedPref.getString("uniqueId","NA"));
                myRef.child("Address").setValue(credentials.getAddress());

            } catch (IOException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (CipherException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (firstLoad) {

                loadingAccount.dismiss();
                settingUp = new ProgressDialog(getContext());
                settingUp.setMessage("Almost Done...");
                settingUp.show();

                boolean unlockedMain = false;

                try {
                    unlockedMain = unlockMainAccount();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    Log.i("--All", "Error: " + e.getMessage());
                    Crashlytics.logException(e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.i("--All", "Error: " + e.getMessage());
                    Crashlytics.logException(e);
                }

                if (unlockedMain) {
                    Log.i("--All", "Successfully Unlocked, now Sending Ether");

                    DatabaseReference myRef2 = database.getReference(sharedPref.getString("uniqueId","NA"));
                    myRef2.child("First Load").push().setValue("Sending Ether to New Account");

                    new SendEtherFromMainAccount().execute();
                }

                ////// New Way
                //sendEtherToThisAccount();

            }

            else new CreateContract().execute();
        }
    }
    public boolean unlockMainAccount() throws ExecutionException, InterruptedException {
        Parity parity = ParityFactory.build(new HttpService("http://45.55.4.74:8545"));  // defaults to http://localhost:8545/
        PersonalUnlockAccount personalUnlockAccount = parity.personalUnlockAccount(MyApplication.mainEtherAddress, "hellya").sendAsync().get();

        if (personalUnlockAccount.accountUnlocked()) {
            Log.i("--All", "Main Account Unlocked with Parity");
            return true;
        } else {
            Log.i("--All", "Error Unlocking Main Account with Parity");
            DatabaseReference myRef = database.getReference("Error");
            myRef.child(sharedPref.getString("uniqueId", "NA")).push().setValue("Version " + BuildConfig.VERSION_NAME + ": " + "Error Unlocking Main Account with Parity - In MainActivity");
            return false;
        }
    }

    public class SendEtherFromMainAccount extends AsyncTask<Object, Void, JSONRPC2Response> {

        @Override
        protected JSONRPC2Response doInBackground(Object... objects) {

            // The JSON-RPC 2.0 server URL
            URL serverURL = null;
            try {serverURL = new URL("http://45.55.4.74:8545");}
            catch (MalformedURLException e) {Log.e("--All", "Error in creating URL: " + e.getMessage());}

            // Create new JSON-RPC 2.0 client session
            JSONRPC2Session mySession = new JSONRPC2Session(serverURL);

            List<Object> listObjects = new ArrayList<>();
            Map mapValues = new HashMap();
            mapValues.put("from", MyApplication.mainEtherAddress);
            mapValues.put("to",sharedPref.getString("ethAddress","none"));
            mapValues.put("value","0xDE0B6B3A7640000");

            listObjects.add(mapValues);

            JSONRPC2Response response = null;
            JSONRPC2Request request = new JSONRPC2Request("eth_sendTransaction", listObjects, 99);

            try {response = mySession.send(request);}
            catch (JSONRPC2SessionException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(JSONRPC2Response response) {

            // Print response result / error
            try {response.indicatesSuccess();
                if (response.indicatesSuccess()) {
                    Log.i("--All", "Successfully Sent Ether");

                    DatabaseReference myRef = database.getReference(sharedPref.getString("uniqueId","NA"));
                    myRef.child("First Load").push().setValue("Sent, now Loading Contract");

                    new CreateContract().execute();

                }
                else{
                    Log.e("--All", "Error in Sending Ether from Main Account: " + response.getError().getMessage());

                    DatabaseReference myRef = database.getReference("Error");
                    myRef.child(sharedPref.getString("uniqueId","NA")).push().setValue("Version " + BuildConfig.VERSION_NAME + ": " + response.getError().toString() + " - In MainActivity");
                    Crashlytics.logException(response.getError());
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
            }
        }
    }

    private class CreateContract extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Log.i("--All", "Creating Contract");
            contract = _Users_Admin_Desktop_Steps_sol_Steps.load(MyApplication.contractAddress, web3, credentials, GAS_PRICE, GAS_LIMIT);

            try {
                Log.i("--All", "Contract Valid: " + contract.isValid());
            } catch (IOException e) {
                Log.i("--All", "Error: " + e.getMessage());
                Crashlytics.logException(e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            for (int i=0; i>=-6; i--) {loadBlockchainData(i);}

            Log.i("--All", "Resetting Alarm After Creating Contract");
            SetAlarm.resetAlarm(getContext());
        }
    }

    public void loadBlockchainData(int day) {

        String formattedDate = DateFormatter.GetConCatDate(day);

        try {
            loadDatesAndSteps(day, formattedDate);
            loadPeopleCount(day, formattedDate);
            loadEveryoneSteps(day, formattedDate);
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.i("--All", "Error: " + e.getMessage());
            Crashlytics.logException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i("--All", "Error: " + e.getMessage());
            Crashlytics.logException(e);
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.i("--All", "Error: " + e.getMessage());
            Crashlytics.logException(e);
        }catch (Exception e) {
            e.printStackTrace();
            Log.i("--All", "Error: " + e.getMessage());
            Crashlytics.logException(e);
        }

        if (firstLoad) settingUp.dismiss();
        else startingDialog.dismiss();

    }

    public void loadEveryoneSteps(int day, String formattedDate) throws ExecutionException, InterruptedException {
        Future<Uint256> everyoneSteps = contract.everyoneStepsDate(new Utf8String(formattedDate));

        int steps = everyoneSteps.get().getValue().intValue();
        int index = day * -1;
        int avgSteps;

        if (peopleCount.get(index) != 0 && peopleCount.size() != 0) avgSteps = steps / peopleCount.get(index);
        else avgSteps = 0;

        if (day == 0 ) avgPeopleSteps0.setText(avgSteps+"");
        if (day == -1 ) avgPeopleSteps1.setText(avgSteps+"");
        if (day == -2 ) avgPeopleSteps2.setText(avgSteps+"");
        if (day == -3 ) avgPeopleSteps3.setText(avgSteps+"");
        if (day == -4 ) avgPeopleSteps4.setText(avgSteps+"");
        if (day == -5 ) avgPeopleSteps5.setText(avgSteps+"");
        if (day == -6 ) avgPeopleSteps6.setText(avgSteps+"");
    }

    public void loadPeopleCount(int day, String formattedDate) throws ExecutionException, InterruptedException {
        Future<Uint256> peopleAllCount = contract.countAllPeopleDate(new Utf8String(formattedDate));

        if (day == 0 ) peopleCount0.setText(peopleAllCount.get().getValue()+"");
        if (day == -1 ) peopleCount1.setText(peopleAllCount.get().getValue()+"");
        if (day == -2 ) peopleCount2.setText(peopleAllCount.get().getValue()+"");
        if (day == -3 ) peopleCount3.setText(peopleAllCount.get().getValue()+"");
        if (day == -4 ) peopleCount4.setText(peopleAllCount.get().getValue()+"");
        if (day == -5 ) peopleCount5.setText(peopleAllCount.get().getValue()+"");
        if (day == -6 ) peopleCount6.setText(peopleAllCount.get().getValue()+"");

        peopleCount.add((day * -1),peopleAllCount.get().getValue().intValue());
    }

    public void loadDatesAndSteps(int day, String formattedDate) throws ExecutionException, InterruptedException {

        Future<Uint256> mySteps = contract.recallMySteps(new Utf8String(formattedDate));

        if (day == 0) {
            //todayStepsBig.setText(i+"");
            day0Steps.setText(mySteps.get().getValue()+"");
            day0Title.setText(DateFormatter.GetMonthYear(day));
        }

        if (day == -1) {
            day1Steps.setText(mySteps.get().getValue()+"");
            day1Title.setText(DateFormatter.GetMonthYear(day));
        }
        if (day == -2) {
            day2Steps.setText(mySteps.get().getValue()+"");
            day2Title.setText(DateFormatter.GetMonthYear(day));
        }
        if (day == -3) {
            day3Steps.setText(mySteps.get().getValue()+"");
            day3Title.setText(DateFormatter.GetMonthYear(day));
        }
        if (day == -4) {
            day4Steps.setText(mySteps.get().getValue()+"");
            day4Title.setText(DateFormatter.GetMonthYear(day));
        }
        if (day == -5) {
            day5Steps.setText(mySteps.get().getValue()+"");
            day5Title.setText(DateFormatter.GetMonthYear(day));
        }
        if (day == -6) {
            day6Steps.setText(mySteps.get().getValue()+"");
            day6Title.setText(DateFormatter.GetMonthYear(day));
        }
    }


    // Define the callback for what to do when data is received
    private BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);
            if (resultCode == RESULT_OK) {
                String resultValue = intent.getStringExtra("resultValue");
                //Toast.makeText(MainActivity.this, "From Activity: " + resultValue, Toast.LENGTH_SHORT).show();
                todayStepsBig.setText(resultValue);
                day0Steps.setText(resultValue);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(StepService.ACTION);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(testReceiver, filter);
        // or `registerReceiver(testReceiver, filter)` for a normal broadcast
        todayStepsBig.setText(StepService.numSteps+"");
        day0Steps.setText(StepService.numSteps+"");
    }
}
