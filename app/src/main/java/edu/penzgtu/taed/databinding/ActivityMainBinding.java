//package edu.penzgtu.taed.databinding;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import androidx.appcompat.widget.Toolbar;
//import androidx.drawerlayout.widget.DrawerLayout;
//import androidx.viewbinding.ViewBinding;
//import com.google.android.material.navigation.NavigationView;
//import edu.penzgtu.taed.R;
//
//public final class ActivityMainBinding implements ViewBinding {
//    public final Toolbar toolbar;
//    public final DrawerLayout drawerLayout;
//    public final NavigationView navView;
//    private final DrawerLayout rootView;
//
//    private ActivityMainBinding(DrawerLayout rootView, Toolbar toolbar, DrawerLayout drawerLayout, NavigationView navView) {
//        this.rootView = rootView;
//        this.toolbar = toolbar;
//        this.drawerLayout = drawerLayout;
//        this.navView = navView;
//    }
//
//    @Override
//    public DrawerLayout getRoot() {
//        return rootView;
//    }
//
//    public static ActivityMainBinding inflate(LayoutInflater inflater) {
//        return inflate(inflater, null, false);
//    }
//
//    public static ActivityMainBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
//        View root = inflater.inflate(R.layout.activity_main, parent, false);
//        if (attachToParent) {
//            parent.addView(root);
//        }
//        return bind(root);
//    }
//
//    public static ActivityMainBinding bind(View rootView) {
//        DrawerLayout drawerLayout = (DrawerLayout) rootView;
//        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
//        NavigationView navView = rootView.findViewById(R.id.nav_view);
//        return new ActivityMainBinding(drawerLayout, toolbar, drawerLayout, navView);
//    }
//}