package zohar.com.ndn_liteble.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

import zohar.com.ndn_liteble.R;
import zohar.com.ndn_liteble.model.Board;

public class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.ViewHolder> {

    private List<Board> boards;

    public BoardAdapter(List<Board> boards) {
        this.boards = boards;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        TextView macAddress;
        TextView identifier;
        TextView kbpub;
        Switch ledSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            macAddress = itemView.findViewById(R.id.tv_mac_address);
            identifier = itemView.findViewById(R.id.tv_identifier);
            kbpub = itemView.findViewById(R.id.tv_kdpub);
            ledSwitch = itemView.findViewById(R.id.switch_led);
        }
    }

    @NonNull
    @Override
    public BoardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = View.inflate(viewGroup.getContext(),R.layout.item_board_node, viewGroup);
        ViewHolder holder = new ViewHolder(view);
        holder.ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHoder, int i) {
        Board board = boards.get(i);
        viewHoder.macAddress.setText(board.getMacAddress());
        viewHoder.identifier.setText(board.getIdentifierHex());
        viewHoder.kbpub.setText(board.getKDPubCertificate());
    }

    @Override
    public int getItemCount() {
        return boards.size();
    }
}
