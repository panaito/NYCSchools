package com.blkxltng.a20200526_mauricegaynor_nycschools.ui.main;

import android.util.Pair;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.blkxltng.a20200526_mauricegaynor_nycschools.dagger.network.DaggerSchoolApiFactory;
import com.blkxltng.a20200526_mauricegaynor_nycschools.models.SatScores;
import com.blkxltng.a20200526_mauricegaynor_nycschools.models.SchoolInfo;
import com.blkxltng.a20200526_mauricegaynor_nycschools.network.SchoolService;
import com.blkxltng.a20200526_mauricegaynor_nycschools.utils.LiveEvent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MainViewModel extends ViewModel {

    @Inject
    public SchoolService schoolService;

    public MainViewModel() {
        DaggerSchoolApiFactory.create().inject(this);
    }

    public MutableLiveData<Integer> progressVisibility = new MutableLiveData<>(View.GONE); // Used to set progressBar visibility
    public MutableLiveData<List<SchoolViewModel>> schoolInfoList = new MutableLiveData<>();
    public LiveEvent<SchoolInfo> clickedSchool = new LiveEvent<>();
    public LiveEvent<Pair<SchoolInfo, SatScores>> schoolScores = new LiveEvent<>();
    public LiveEvent<SchoolErrorCode> errorCode = new LiveEvent<>(); // Used when there is an error to send the user a message

    public void loadSchools() {
        Observable<List<SchoolInfo>> schools = schoolService.getSchools();
        progressVisibility.postValue(View.VISIBLE);

        schools
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<List<SchoolInfo>>() {
                    @Override
                    public void onNext(@NonNull List<SchoolInfo> schoolInfos) {
                        List<SchoolViewModel> vmList = new ArrayList<>();
                        for(SchoolInfo info : schoolInfos) {
                            vmList.add(new SchoolViewModel(info, MainViewModel.this));
                        }
                        schoolInfoList.postValue(vmList);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        progressVisibility.postValue(View.GONE);
                        Timber.d(e);
                        errorCode.postValue(SchoolErrorCode.ERROR_SCHOOL);
                    }

                    @Override
                    public void onComplete() {
                        progressVisibility.postValue(View.GONE);
                    }
                });
    }

    public void getScores(String dbn) {
        Observable<List<SatScores>> scores = schoolService.getScores(dbn);
        progressVisibility.postValue(View.VISIBLE);

        scores
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<List<SatScores>>() {
                    @Override
                    public void onNext(@NonNull List<SatScores> scores) {
                        if(scores.size() > 0) {
                            // Return the school info and SAT scores
                            schoolScores.postValue(new Pair<>(clickedSchool.getValue(), scores.get(0)));
                        } else {
                            // No SAT scores could be found for this school. Just return the school info
                            schoolScores.postValue(new Pair<>(clickedSchool.getValue(), null));
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        // Display an error of something goes wrong when obtaining SAT scores
                        Timber.d(e);
                        progressVisibility.postValue(View.GONE);
                        errorCode.postValue(SchoolErrorCode.ERROR_SCORES);
                    }

                    @Override
                    public void onComplete() {
                        progressVisibility.postValue(View.GONE);
                    }
                });
    }
}

enum SchoolErrorCode {
    NOT_FOUND, NO_CONNECTION, ERROR_SCHOOL, ERROR_SCORES
}
