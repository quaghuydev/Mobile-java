package com.quanghuydev.chatapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.quanghuydev.chatapp.adapters.ChatAdapter;
import com.quanghuydev.chatapp.databinding.ActivityChatBinding;
import com.quanghuydev.chatapp.models.ChatMessage;
import com.quanghuydev.chatapp.models.User;
import com.quanghuydev.chatapp.network.ApiClient;
import com.quanghuydev.chatapp.network.ApiService;
import com.quanghuydev.chatapp.ultilities.Contants;
import com.quanghuydev.chatapp.ultilities.PreferenceManager;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
//import java.util.Objects;

public class ChatActivity extends BaseActivity {
    private ActivityChatBinding binding;
    private User recivedUser;
    private List<ChatMessage>chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
        loadReceiverDetail();
        init();
        listenMessage();
    }
    private void init(){
        preferenceManager= new PreferenceManager(getApplicationContext());
        chatMessages= new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages,getBitMapFromEncodeString(recivedUser.image),preferenceManager.getString(Contants.KEY_USER_ID));
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database= FirebaseFirestore.getInstance();
    }
    private void sendMessage(){
        HashMap<String,Object>message=new HashMap<>();
        message.put(Contants.KEY_SEND_ID,preferenceManager.getString(Contants.KEY_USER_ID));
        message.put(Contants.KEY_RECEIVER_ID,recivedUser.id);
        message.put(Contants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(Contants.KEY_TIMESTAMP,new Date());
        database.collection(Contants.KEY_COLLECTION_CHAT).add(message);
        if(conversionId != null){
            updateConversion(binding.inputMessage.getText().toString());
        }else{
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Contants.KEY_SEND_ID, preferenceManager.getString(Contants.KEY_USER_ID));
            conversion.put(Contants.KEY_SENDER_NAME, preferenceManager.getString(Contants.KEY_NAME));
            conversion.put(Contants.KEY_SENDER_IMAGE, preferenceManager.getString(Contants.KEY_IMAGE));
            conversion.put(Contants.KEY_RECEIVER_ID, recivedUser.id);
            conversion.put(Contants.KEY_RECEIVER_NAME, recivedUser.name);
            conversion.put(Contants.KEY_RECEIVER_IMAGE, recivedUser.image);
            conversion.put(Contants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Contants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if(!isReceiverAvailable){
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(recivedUser.token);

                JSONObject data = new JSONObject();
                data.put(Contants.KEY_USER_ID, preferenceManager.getString(Contants.KEY_USER_ID));
                data.put(Contants.KEY_NAME, preferenceManager.getString(Contants.KEY_NAME));
                data.put(Contants.KEY_FCM_TOKEN, preferenceManager.getString(Contants.KEY_FCM_TOKEN));
                data.put(Contants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Contants.REMOTE_MSG_DATA, data);
                body.put(Contants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        binding.inputMessage.setText(null);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Contants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if(response.isSuccessful()){
                    try {
                        if(response.body() != null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                }else{
//                    showToast("Error: "+response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void listenAvailabilityOfReceiver(){
        database.collection(Contants.KEY_COLLECTION_USERS).document(
                recivedUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) ->{
           if(error != null){
               return;
           }
           if(value != null){
               if(value.getLong(Contants.KEY_AVAILABILITY) != null){
                   int availability = Objects.requireNonNull(
                           value.getLong(Contants.KEY_AVAILABILITY)
                   ).intValue();
                   isReceiverAvailable = availability == 1;
               }
               recivedUser.token = value.getString(Contants.KEY_FCM_TOKEN);
               if(recivedUser.image == null){
                   recivedUser.image = value.getString(Contants.KEY_IMAGE);
                   chatAdapter.setReceiveProfileImage(getBitMapFromEncodeString(recivedUser.image));
                   chatAdapter.notifyItemRangeChanged(0, chatMessages.size());
               }
           }
           if(isReceiverAvailable){
               binding.textAvailability.setVisibility(View.VISIBLE);
           }else{
               binding.textAvailability.setVisibility(View.GONE);
           }
        });
    }

    private void listenMessage(){
        database.collection(Contants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Contants.KEY_SEND_ID,preferenceManager.getString(Contants.KEY_USER_ID))
                .whereEqualTo(Contants.KEY_RECEIVER_ID,recivedUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Contants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Contants.KEY_SEND_ID,recivedUser.id)
                .whereEqualTo(Contants.KEY_RECEIVER_ID,preferenceManager.getString(Contants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

    }
    private final EventListener<QuerySnapshot>eventListener=((value, error) -> {
        if(error!=null){
            return;
        }if(value!=null){
            int count = chatMessages.size();
            for (DocumentChange documentChange: value.getDocumentChanges()){
               if(documentChange.getType()==DocumentChange.Type.ADDED){
                   ChatMessage chatMessage = new ChatMessage();
                   chatMessage.senderId = documentChange.getDocument().getString(Contants.KEY_SEND_ID);
                   chatMessage.recieveId = documentChange.getDocument().getString(Contants.KEY_RECEIVER_ID);
                   chatMessage.message = documentChange.getDocument().getString(Contants.KEY_MESSAGE);
                   chatMessage.dateTime = getReadaleDateTime(documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP));
                   chatMessage.dateObject = documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP);
                   chatMessages.add(chatMessage);
               }

            }
            Collections.sort(chatMessages,(obj1,obj2)->obj1.dateObject.compareTo(obj2.dateObject));
            if(count==0){
                chatAdapter.notifyDataSetChanged();
            }else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);

            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversionId == null){
            checkForConversion();
        }
    });
    private Bitmap getBitMapFromEncodeString(String encode){
        if(encode != null){
            byte[]bytes = Base64.decode(encode,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }else{
            return null;
        }
    }


    private void loadReceiverDetail() {
        recivedUser = (User) getIntent().getSerializableExtra(Contants.KEY_USER);
        binding.textName.setText(recivedUser.name);
    }
    private void setListener(){
        binding.imageBack.setOnClickListener(v->onBackPressed());
        binding.layoutSend.setOnClickListener(view -> sendMessage());
    }
    private String getReadaleDateTime(Date date){
        return  new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String,Object> conversion){
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message){
        DocumentReference documentReference =
                database.collection(Contants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Contants.KEY_LAST_MESSAGE, message,
                Contants.KEY_TIMESTAMP, new Date()
        );
    }
    private void checkForConversion(){
        if(chatMessages.size() != 0){
            checkForConversionRemotely(
                    preferenceManager.getString(Contants.KEY_USER_ID),
                    recivedUser.id
            );
            checkForConversionRemotely(
                    recivedUser.id,
                    preferenceManager.getString(Contants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Contants.KEY_SEND_ID, senderId)
                .whereEqualTo(Contants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
      if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
          DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
          conversionId = documentSnapshot.getId();
      }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}