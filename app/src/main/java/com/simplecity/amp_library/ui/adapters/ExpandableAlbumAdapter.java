package com.simplecity.amp_library.ui.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.fragments.ArtistDetailFragment;
import com.simplecity.amp_library.ui.modelviews.ExpandableAlbumView;
import com.simplecity.amp_library.ui.modelviews.MultiItemView;
import com.simplecity.amp_library.ui.modelviews.SongView;

import java.util.ArrayList;
import java.util.List;

public class ExpandableAlbumAdapter extends ExpandableRecyclerAdapter<Parent<Object>, Object, ParentViewHolder<Parent<Object>, Object>, ExpandableAlbumAdapter.ChildSongViewHolder> {

    /**
     * Primary constructor. Sets up {@link #mParentList} and {@link #mFlatItemList}.
     *
     * @param parentList List of all parents to be displayed in the RecyclerView that this
     *                   adapter is linked to
     */
    public ExpandableAlbumAdapter(@NonNull List<Parent<Object>> parentList) {
        super(parentList);
    }

    public interface ViewType {
        int HEADER = 3;
        int ALBUM = 4;
    }

    @Override
    public boolean isParentViewType(int viewType) {
        return viewType == ViewType.HEADER || viewType == ViewType.ALBUM;
    }

    @Override
    public int getParentViewType(int parentPosition) {
        if (parentPosition == 0) {
            return ViewType.HEADER;
        }
        return ViewType.ALBUM;
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateParentViewHolder(@NonNull ViewGroup parentViewGroup, int viewType) {
        switch (viewType) {
            case ViewType.HEADER:
                return new ParentHeaderViewHolder(LayoutInflater.from(parentViewGroup.getContext()).inflate(R.layout.list_item_image, parentViewGroup, false));
            case ViewType.ALBUM:
                return new ParentAlbumViewHolder(LayoutInflater.from(parentViewGroup.getContext()).inflate(R.layout.list_item_expandable_header, parentViewGroup, false));
        }
        throw new IllegalStateException("onCreateParentViewHolder() called for invalid viewType: " + viewType);
    }

    @NonNull
    @Override
    public ChildSongViewHolder onCreateChildViewHolder(@NonNull ViewGroup childViewGroup, int viewType) {
        return new ChildSongViewHolder(LayoutInflater.from(childViewGroup.getContext()).inflate(R.layout.list_item_two_lines, childViewGroup, false));
    }

    @Override
    public void onBindParentViewHolder(@NonNull ParentViewHolder parentViewHolder, int parentPosition, @NonNull Parent parent) {

        if (parent instanceof Header) {
            ((Header) parent).headerView.bindView(parentViewHolder);
        } else if (parent instanceof ExpandableAlbum) {
            ((ExpandableAlbum) parent).albumView.bindView(((ParentAlbumViewHolder) parentViewHolder).holder);
        }
    }

    @Override
    public void onBindChildViewHolder(@NonNull ChildSongViewHolder childViewHolder, int parentPosition, int childPosition, @NonNull Object child) {
        if (child instanceof SongView) {
            ((SongView) child).bindView(childViewHolder.holder);
        }
    }

    public static class Header implements Parent<Object> {

        public ArtistDetailFragment.HeaderView headerView;

        public Header(ArtistDetailFragment.HeaderView headerView) {
            this.headerView = headerView;
        }

        @Override
        public List<Object> getChildList() {
            return new ArrayList<>();
        }

        @Override
        public boolean isInitiallyExpanded() {
            return false;
        }
    }

    public static class ExpandableAlbum implements Parent<SongView> {

        ExpandableAlbumView albumView;

        List<SongView> songViews = new ArrayList<>();

        public ExpandableAlbum(ExpandableAlbumView albumView, List<SongView> songViews) {
            this.albumView = albumView;
            this.songViews = songViews;
        }

        @Override
        public List<SongView> getChildList() {
            return songViews;
        }

        @Override
        public boolean isInitiallyExpanded() {
            return true;
        }
    }

    public static class ParentHeaderViewHolder extends ParentViewHolder {

        RecyclerView.ViewHolder holder;

        /**
         * Default constructor.
         *
         * @param itemView The {@link View} being hosted in this ViewHolder
         */
        public ParentHeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            holder = new ArtistDetailFragment.HeaderView.ViewHolder(itemView);
        }
    }

    public static class ParentAlbumViewHolder extends ParentViewHolder<ExpandableAlbum, SongView> {

        MultiItemView.ViewHolder holder;

        /**
         * Default constructor.
         *
         * @param itemView The {@link View} being hosted in this ViewHolder
         */
        public ParentAlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            holder = new MultiItemView.ViewHolder(itemView, com.simplecity.amp_library.ui.modelviews.ViewType.ALBUM_LIST, null);
        }
    }

    public static class ChildSongViewHolder extends ChildViewHolder<SongView> {

        SongView.ViewHolder holder;

        /**
         * Default constructor.
         *
         * @param itemView The {@link View} being hosted in this ViewHolder
         */
        public ChildSongViewHolder(@NonNull View itemView) {
            super(itemView);
            holder = new SongView.ViewHolder(itemView, null);
        }
    }
}