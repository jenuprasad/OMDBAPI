package omdb.jenuine.com.omdbapi;

import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;


public class MainActivity extends BaseActivity implements MovieView<Movie>, SearchView.OnQueryTextListener {


    private ActivityMainPresenter presenter;
    private SearchView search;
    private MovieAdapter adapter;
    private FrameLayout progress_layout;
    private ActivityMainPresenter2 presenter2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.presenter = ActivityMainPresenter.create(this);
        this.presenter2 = ActivityMainPresenter2.create(this);
        this.adapter = new MovieAdapter(this.findViewById(R.id.empty));
        final RecyclerView recycler = (RecyclerView) this.findViewById(R.id.movies);
        recycler.setAdapter(this.adapter);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(new SlideInOutTopItemAnimator(recycler));

        adapter.setOnClickListener(new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ImageView poster = (ImageView) view.findViewById(R.id.poster);
                launch(poster, position);
            }
        });


        progress_layout = (FrameLayout) findViewById(R.id.progress_layout);
    }

    private void launch(ImageView ivProfile, int position) {
        Intent intent = new Intent(this, DetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(DetailActivity.EXTRA_IMDB_DATA, adapter.getRepos().get(position));
        intent.putExtras(bundle);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this, (View) ivProfile, "profile");
        startActivity(intent, options.toBundle());
    }

    private void launchWithMovie(Movie movie) {
        Intent intent = new Intent(this, DetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(DetailActivity.EXTRA_IMDB_DATA, movie);
        intent.putExtras(bundle);
//        ActivityOptionsCompat options = ActivityOptionsCompat.
//                makeSceneTransitionAnimation(this, (View) ivProfile, "profile");
        startActivity(intent);
    }

    @Override
    protected int onCreateViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected int onCreateViewToolbarId() {
        return R.id.toolbar;
    }

    private SimpleCursorAdapter suggestionsAdapter;

    private static Movie[] MOVIES = new Movie[]{};

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        this.search = (SearchView) MenuItemCompat.getActionView(item);
        final String[] from = new String[]{"movieTitle"};
        final int[] to = new int[]{android.R.id.text1};
        suggestionsAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                from,
                to,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        search.setSuggestionsAdapter(suggestionsAdapter);
        this.search.setOnQueryTextListener(this);
        this.search.onActionViewExpanded();
        this.search.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionClick(int position) {
                // Your code here
                MatrixCursor cursor = (MatrixCursor) suggestionsAdapter.getCursor();
                cursor.moveToPosition(position);
                String title = cursor.getString(1);
                Log.v("MainActivity", title + "");
                title = StringUtils.substringBefore(title, "(");
                Movie movie = adapter.getMovieByTitle(title);
                if (movie != null) {
                    launchWithMovie(movie);
                } else {
                    presenter.getMovie(title);
                    search.clearFocus();
                    progress_layout.setVisibility(View.VISIBLE);
                }
                return true;
            }

            @Override
            public boolean onSuggestionSelect(int position) {
                // Your code here
                MatrixCursor cursor = (MatrixCursor) suggestionsAdapter.getCursor();
                cursor.moveToPosition(position);
                String title = cursor.getString(1);
                Log.v("MainActivity", title + "");
                title = StringUtils.substringBefore(title, "(");
                Movie movie = adapter.getMovieByTitle(title);
                if (movie != null) {
                    launchWithMovie(movie);
                } else {
                    title = StringUtils.substringBefore(title, "(");
                    presenter.getMovie(title);
                    search.clearFocus();
                    progress_layout.setVisibility(View.VISIBLE);
                }

                return true;
            }
        });
        this.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Movie movie = adapter.getMovieByTitle(query);
                if (movie != null) {
                    launchWithMovie(movie);
                } else {
                    presenter.getMovie(query);
                    search.clearFocus();
                    progress_layout.setVisibility(View.VISIBLE);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchMovies(s);
                return false;
            }
        });
        return true;
    }

    private void searchMovies(String query) {
        this.presenter2.getMovies(query);
    }

    private void populateAdapter(Movie[] query) {
        // You must implements your logic to get data using OrmLite
        final MatrixCursor c = new MatrixCursor(new String[]{BaseColumns._ID, "movieTitle"});
        for (int i = 0; i < query.length; i++) {
            c.addRow(new Object[]{i, query[i].getTitle() + " (" + query[i].getYear() + ")"});
        }
        suggestionsAdapter.changeCursor(c);
    }

    @Override
    public void toast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onQueryTextSubmit(final String query) {
        this.presenter.getMovie(query);
        this.search.clearFocus();
        progress_layout.setVisibility(View.VISIBLE);
        return true;
    }

    @Override
    public boolean onQueryTextChange(final String query) {
        return true;
    }

    @Override
    public void addItem(Movie item) {
        adapter.addItem(item);
        progress_layout.setVisibility(View.GONE);
    }

    @Override
    public void addItems(Movie[] items) {
        MOVIES = items;
        populateAdapter(items);
    }

    private void showToast(Movie item) {
        Toast.makeText(MainActivity.this, item + "", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void clearItem() {

    }
}

