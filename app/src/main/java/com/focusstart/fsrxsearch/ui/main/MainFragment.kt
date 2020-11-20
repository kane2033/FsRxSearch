package com.focusstart.fsrxsearch.ui.main

import android.content.res.Resources
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.main_fragment.*
import androidx.annotation.RawRes
import com.focusstart.fsrxsearch.R
import com.focusstart.fsrxsearch.RxSearchObservable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MainFragment : Fragment() {

    private val disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.main_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storyView.movementMethod = ScrollingMovementMethod() // Скроллинг текста

        val rawId = R.raw.chekhov_surgery_story // Небольшой файл
        //val rawId = R.raw.moby_dick // Очень большой файл

        // Получаем рассказ из текстового файла асинхронно
        disposables.add( // Нужно ли добавлять Single в disposables?
                Single.just(resources.getRawTextFile(rawId))
                        .subscribeOn(Schedulers.io()) // Работу выполняем в io
                        .observeOn(AndroidSchedulers.mainThread()) // Subscribe работает в мейн треде
                        .subscribe { text -> storyView.text = text }
        )

        // Реализация поиска
        disposables.add(
                RxSearchObservable.fromView(searchView)
                        .subscribeOn(Schedulers.io()) // Работу выполняем в io
                        .debounce(700L, TimeUnit.MILLISECONDS)
                        .distinctUntilChanged() // Отсеивает повторяющиеся последовательно переданные эл-ы
                        .filter { it.isNotEmpty() } // Не пропускаем пустую строку
                        .switchMap { query -> // switchMap почему-то не откидывает новые запросы
                            // Строку в поиске отправляет в метод подсчета
                            countMatchesInRawRes(query, rawId)
                        }
                        .observeOn(AndroidSchedulers.mainThread()) // Subscribe работает в мейн треде
                        .subscribe {
                            matchesNumberView.text = it.toString()
                        }
        )
    }

    // Подсчет совпадений в текстовом файле (файл открывается как поток)
    private fun countMatchesInRawRes(query: String, @RawRes fullTextId: Int) =
            Flowable.create<Int>({ emitter ->
                resources.openRawResource(fullTextId).bufferedReader().use { bufferedReader ->
                    var count = 0
                    bufferedReader.forEachLine { line ->
                        count += query.toRegex(RegexOption.IGNORE_CASE).findAll(line).count()
                        emitter.onNext(count)
                    }
                }
                emitter.onComplete()
            }, BackpressureStrategy.BUFFER)


    // Возвращает single с выполнением подсчета совпадений в строке
    private fun countMatchesInString(query: String, fullText: String) =
            Single.just(fullText.countMatches(query).toString())

    // Подсчет совпадений в строке
    private fun String.countMatches(query: String): Int {
        val matcher = Pattern.compile(query, Pattern.CASE_INSENSITIVE).matcher(this)
        return count { matcher.find() }
    }

    // Экстеншн для получения строки из raw ресурса
    private fun Resources.getRawTextFile(@RawRes id: Int) =
            openRawResource(id).bufferedReader().use { it.readText() }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }
}