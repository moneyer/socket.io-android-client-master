package com.centersoft.chat;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.centersoft.base.BaseActivity;
import com.centersoft.util.MyLog;
import com.zhy.adapter.abslistview.CommonAdapter;
import com.zhy.adapter.abslistview.ViewHolder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by fengli on 2018/2/27.
 */

public class LocationActy extends BaseActivity {

    @BindView(R.id.map_view)
    MapView mapView;

    private AMap aMap;
    private boolean firstLocated = true;
    private LatLng oldLocation;
    private GeocodeSearch geocodeSearch;
    private String currentDetailLocationName;
    private String currentLocationName;
    private int selectedIndex = 0;

    private List aroundLocationList = new ArrayList();

    private CommonAdapter adapter;

    private boolean userMoveCamera = true;

    @BindView(R.id.map_hot_list)
    ListView hotList;

    @OnClick({R.id.map_relocated_btn, R.id.tv_send})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.map_relocated_btn:
                Location location = aMap.getMyLocation();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                cameraMove(latLng);

                addLocationPinAnimation();
                searchLocationsName(latLng);
                break;
            case R.id.tv_send:
                sendLocation();
                break;
        }
    }


    @BindView(R.id.map_location_pin)
    ImageView locationPin;

    @Override
    protected void setMyToolBar() {
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
    }

    @Override
    protected void initData() {


        aMap = mapView.getMap();

        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000);
        myLocationStyle.showMyLocation(true);
        myLocationStyle.anchor((float) 0.5, 1);
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);

        /*
        * 定位更新
        * */
        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {

                if (firstLocated) {
                    firstLocated = false;

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    cameraMove(latLng);
                    addLocationPinAnimation();
                    searchLocationsName(latLng);
                }
            }
        });

        /*
        * 地图显示区域变化
        * */
        aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
                if (userMoveCamera) { // 用户操作
                    LatLng latLng = cameraPosition.target;
                    addLocationPinAnimation();
                    searchLocationsName(latLng);
                }
            }
        });

        geocodeSearch = new GeocodeSearch(context);
        geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
                List<PoiItem> poiItemList = address.getPois();
                currentDetailLocationName = address.getFormatAddress();
                currentLocationName = address.getAois().get(0).getAoiName();

                aroundLocationList.clear();
                aroundLocationList.add(currentDetailLocationName);
                aroundLocationList.addAll(poiItemList);
                adapter.notifyDataSetChanged();
                selectedIndex = 0;

            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

                MyLog.i("world");
            }
        });

        adapter = new CommonAdapter(context, R.layout.acty_location_detail_item, aroundLocationList) {
            @Override
            protected void convert(ViewHolder viewHolder, Object item, int position) {

                TextView detailAddress = viewHolder.getView(R.id.location_detail_address);
                ImageView selectIcon = viewHolder.getView(R.id.location_select_icon);
                if (item instanceof String) {
                    viewHolder.setText(R.id.location_address, (String) item);
                    detailAddress.setVisibility(View.GONE);
                } else {
                    detailAddress.setVisibility(View.VISIBLE);
                    PoiItem poiItem = (PoiItem) item;

                    viewHolder.setText(R.id.location_address, poiItem.getTitle());
                    viewHolder.setText(R.id.location_detail_address, poiItem.getSnippet());
                }
                if (selectedIndex == position) {
                    selectIcon.setVisibility(View.VISIBLE);
                } else {
                    selectIcon.setVisibility(View.GONE);
                }
            }
        };
        hotList.setAdapter(adapter);

        hotList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (selectedIndex == i) {
                    return;
                }

                selectedIndex = i;
                if (i == 0) {

                    cameraMove(oldLocation);
                } else {
                    PoiItem poiItem = (PoiItem) aroundLocationList.get(i);
                    LatLonPoint point = poiItem.getLatLonPoint();
                    LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
                    cameraMove(latLng);
                    currentLocationName = poiItem.getTitle();
                    currentDetailLocationName = poiItem.getSnippet();
                }
                adapter.notifyDataSetChanged();
                addLocationPinAnimation();
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapView.onCreate(savedInstanceState);
    }

    @Override
    protected int initResource() {
        return R.layout.acty_location;
    }

    private void addLocationPinAnimation() {

        Animation animation = AnimationUtils.loadAnimation(context, R.anim.location_pin_anim);
        locationPin.startAnimation(animation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /*
    * 移动地图中心点
    * */
    private void cameraMove(LatLng latLng) {

        userMoveCamera = false;
        CameraUpdateFactory factory = new CameraUpdateFactory();
        CameraUpdate cameraUpdate = factory.newLatLngZoom(latLng, (float) 15.5);
        aMap.animateCamera(cameraUpdate, new AMap.CancelableCallback() {
            @Override
            public void onFinish() {
                userMoveCamera = true;
            }

            @Override
            public void onCancel() {
                userMoveCamera = true;
            }
        });
    }

    /*
    * 发送位置
    * */
    private void sendLocation() {

        double lat = 0.0;
        double lon = 0.0;
        if (selectedIndex == 0) {
            lat = oldLocation.latitude;
            lon = oldLocation.longitude;
        } else {

            PoiItem poiItem = (PoiItem) aroundLocationList.get(selectedIndex);
            lat = poiItem.getLatLonPoint().getLatitude();
            lon = poiItem.getLatLonPoint().getLongitude();
        }

        Intent intent = new Intent();
        intent.putExtra("lat", lat);
        intent.putExtra("lon", lon);
        intent.putExtra("location", currentLocationName);
        intent.putExtra("detailLocation", currentDetailLocationName);
        setResult(101, intent);
        goBack();
    }

    /*
    * 根据位置搜索附近热点信息
    * */
    private void searchLocationsName(LatLng latLng) {
        oldLocation = latLng;
        LatLonPoint point = new LatLonPoint(latLng.latitude, latLng.longitude);
        RegeocodeQuery query = new RegeocodeQuery(point, 1000, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
    }


}
