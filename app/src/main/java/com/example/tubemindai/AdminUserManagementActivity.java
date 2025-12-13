package com.example.tubemindai;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.AdminUserAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.AdminUsersResponse;
import com.example.tubemindai.api.models.AdminUserActionResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserManagementActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView rvUsers;
    private LinearLayout llEmptyState;
    private TextInputEditText etSearch;
    private AdminUserAdapter userAdapter;
    private List<AdminUsersResponse.AdminUserItem> userList;
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;
    private int currentPage = 0;
    private final int PAGE_SIZE = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_management);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        loadUsers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvUsers = findViewById(R.id.rvUsers);
        llEmptyState = findViewById(R.id.llEmptyState);
        etSearch = findViewById(R.id.etSearch);
        apiService = ApiClient.getApiService();
        prefsManager = new SharedPrefsManager(this);
        userList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("User Management");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        userAdapter = new AdminUserAdapter(userList);
        userAdapter.setOnUserClickListener(user -> {
            // Show user details dialog
            showUserDetailsDialog(user);
        });
        userAdapter.setOnActivateClickListener((user, position) -> {
            // Activate/Deactivate user
            showActivateDialog(user, position);
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Reload with search query
                currentPage = 0;
                loadUsers();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showProgressDialog("Loading users...");

        String authHeader = "Bearer " + accessToken;
        String searchQuery = etSearch.getText().toString().trim();
        String search = searchQuery.isEmpty() ? null : searchQuery;

        Call<AdminUsersResponse> call = apiService.getAllUsers(
            authHeader,
            currentPage * PAGE_SIZE,
            PAGE_SIZE,
            search,
            null,
            null
        );

        call.enqueue(new Callback<AdminUsersResponse>() {
            @Override
            public void onResponse(Call<AdminUsersResponse> call, Response<AdminUsersResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    AdminUsersResponse usersResponse = response.body();
                    List<AdminUsersResponse.AdminUserItem> users = usersResponse.getUsers();

                    if (currentPage == 0) {
                        userList.clear();
                    }
                    userList.addAll(users);
                    userAdapter.notifyDataSetChanged();

                    if (userList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    String errorMsg = "Failed to load users";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            com.example.tubemindai.api.models.ApiError error =
                                new Gson().fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                            if (error != null && error.getDetail() != null) {
                                errorMsg = error.getDetail();
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    Toast.makeText(AdminUserManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AdminUsersResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(AdminUserManagementActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showUserDetailsDialog(AdminUsersResponse.AdminUserItem user) {
        new AlertDialog.Builder(this)
            .setTitle("User Details")
            .setMessage(
                "Name: " + user.getName() + "\n\n" +
                "Email: " + user.getEmail() + "\n\n" +
                "Status: " + (user.isActive() ? "Active" : "Inactive") + "\n" +
                "Verified: " + (user.isVerified() ? "Yes" : "No") + "\n" +
                "Admin: " + (user.isAdmin() ? "Yes" : "No") + "\n\n" +
                "Videos: " + user.getVideoCount() + "\n" +
                "Chats: " + user.getChatCount() + "\n\n" +
                "Created: " + (user.getCreatedAt() != null ? user.getCreatedAt() : "N/A")
            )
            .setPositiveButton("OK", null)
            .show();
    }

    private void showActivateDialog(AdminUsersResponse.AdminUserItem user, int position) {
        String action = user.isActive() ? "deactivate" : "activate";
        new AlertDialog.Builder(this)
            .setTitle("Confirm Action")
            .setMessage("Are you sure you want to " + action + " this user?")
            .setPositiveButton("Yes", (dialog, which) -> activateUser(user, position))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void activateUser(AdminUsersResponse.AdminUserItem user, int position) {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Updating user...");

        String authHeader = "Bearer " + accessToken;
        Call<AdminUserActionResponse> call = apiService.activateUser(authHeader, user.getId());

        call.enqueue(new Callback<AdminUserActionResponse>() {
            @Override
            public void onResponse(Call<AdminUserActionResponse> call, Response<AdminUserActionResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    AdminUserActionResponse actionResponse = response.body();
                    // Update user in list
                    user.setActive(actionResponse.isActive());
                    userAdapter.updateUser(position, user);
                    Toast.makeText(AdminUserManagementActivity.this,
                        actionResponse.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = "Failed to update user";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            com.example.tubemindai.api.models.ApiError error =
                                new Gson().fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                            if (error != null && error.getDetail() != null) {
                                errorMsg = error.getDetail();
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    Toast.makeText(AdminUserManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AdminUserActionResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(AdminUserManagementActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEmptyState() {
        rvUsers.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvUsers.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            currentPage = 0;
            loadUsers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
