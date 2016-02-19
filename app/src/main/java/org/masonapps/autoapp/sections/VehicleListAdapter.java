package org.masonapps.autoapp.sections;

import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.masonapps.autoapp.R;
import org.masonapps.autoapp.database.AutoEntry;

import java.util.ArrayList;


public class VehicleListAdapter extends RecyclerView.Adapter<VehicleListAdapter.ViewHolder> {

    public final ArrayList<AutoEntry> autoEntries;
    private final VehicleListFragment.OnVehicleInteractionListener listener;

    public VehicleListAdapter(ArrayList<AutoEntry> autoEntries, VehicleListFragment.OnVehicleInteractionListener listener) {
        this.autoEntries = autoEntries;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final AutoEntry entry = autoEntries.get(position);
        holder.mItem = entry;
        holder.descriptionText.setText(String.format("%d %s %s", entry.year, entry.make, entry.model));
        holder.trimText.setText(String.format("Trim: %s", entry.trim));
    }

    @Override
    public int getItemCount() {
        return autoEntries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public AutoEntry mItem;
        public TextView descriptionText;
        public TextView trimText;
        private ImageButton menuButton;

        public ViewHolder(View view) {
            super(view);
            descriptionText = (TextView) view.findViewById(R.id.descriptionText);
            trimText = (TextView) view.findViewById(R.id.trimText);
            menuButton = (ImageButton) view.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.inflate(R.menu.vehicle_list_item_menu);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()){
                                case R.id.action_delete:
                                    listener.onDeleteClicked(autoEntries.get(getAdapterPosition()));
                                    autoEntries.remove(getAdapterPosition());
                                    notifyDataSetChanged();
                                    break;
                                case R.id.action_edit:
                                    listener.onEditClicked(autoEntries.get(getAdapterPosition()));
                                    break;
                            }
                            return false;
                        }
                    });
                    popupMenu.show();
                }
            });
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
