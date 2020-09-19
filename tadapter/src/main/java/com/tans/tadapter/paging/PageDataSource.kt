package com.tans.tadapter.paging

import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class PageDataSource<P, T> private constructor(
    private val dataSourceType: DataSourceType<P, T>
) : PageKeyedDataSource<P, T>() {

    var willLoadParam: P? = null

    override fun loadInitial(
        params: LoadInitialParams<P>,
        callback: LoadInitialCallback<P, T>
    ) {
        when (dataSourceType) {
            is DataSourceType.New -> {
                callback.onResult(
                    dataSourceType.dataGetter(dataSourceType.initParams),
                    null,
                    dataSourceType.nextPageParams(dataSourceType.initParams).apply { this@PageDataSource.willLoadParam = this }
                )
            }

            is DataSourceType.Modify -> {

                callback.onResult(
                    dataSourceType.modifiedItems,
                    null,
                    dataSourceType.nextPageParam.apply { this@PageDataSource.willLoadParam = this }
                )
            }
        }
    }

    override fun loadAfter(params: LoadParams<P>, callback: LoadCallback<P, T>) {
        callback.onResult(dataSourceType.dataGetter(params.key), dataSourceType.nextPageParams(params.key))
    }

    override fun loadBefore(params: LoadParams<P>, callback: LoadCallback<P, T>) {
    }

    sealed class DataSourceType<P, T>(val nextPageParams: (P) -> P, val dataGetter: (P) -> List<T>) {

        class New<P, T>(
            val initParams: P,
            nextPageParams: (P) -> P,
            dataGetter: (P) -> List<T>
        ) : DataSourceType<P, T>(nextPageParams, dataGetter)

        class Modify<P, T>(
            val nextPageParam: P,
            val modifiedItems: List<T>,
            nextPageParams: (P) -> P,
            dataGetter: (P) -> List<T>
        ) : DataSourceType<P, T>(nextPageParams, dataGetter)

    }

    class PageDataSourceFactory<P, T>(private val newType: DataSourceType.New<P, T>) : DataSource.Factory<P, T>() {

        private val dataSources: Subject<PageDataSource<P, T>> = BehaviorSubject
            .createDefault<PageDataSource<P, T>>(PageDataSource(newType))
            .toSerialized()

        override fun create(): DataSource<P, T> = dataSources.firstElement().blockingGet()

        fun refresh(): Completable = dataSources.firstElement()
            .flatMapCompletable { dataSource ->
                Completable.fromAction {
                    this.dataSources.onNext(PageDataSource(newType))
                    dataSource.invalidate()
                }
            }

        fun modify(modifiedItems: List<T>): Completable = dataSources.firstElement()
            .flatMapCompletable { dataSource ->
                Completable.fromAction {
                    this.dataSources.onNext(PageDataSource(dataSourceType = DataSourceType.Modify(
                        nextPageParams = newType.nextPageParams,
                        nextPageParam = dataSource.willLoadParam!!,
                        dataGetter = newType.dataGetter,
                        modifiedItems = modifiedItems
                    )))
                    dataSource.invalidate()
                }
            }

    }

}