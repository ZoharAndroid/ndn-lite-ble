package zohar.com.ndn_liteble.adapter;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;


import java.io.IOException;
import java.util.List;

import NDNLiteSupport.BLEFace.BLEFace;
import zohar.com.ndn_liteble.DeviceFragment;
import zohar.com.ndn_liteble.R;
import zohar.com.ndn_liteble.model.Board;
import zohar.com.ndn_liteble.utils.Constant;
import zohar.com.ndn_liteble.utils.SendInterestTask;

public class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.ViewHolder> {

    private static final String TAG = "BoardAdapter";

    // 填充的数据
    private List<Board> boards;

    private OnClickBoardImageListener clickBoardImageListener;


    public interface OnClickBoardImageListener{
         void onClickBoardImageListener(View view, int position);
    }

    public void setOnClickBoardImageListener(OnClickBoardImageListener clickBoardImageListener){
        this.clickBoardImageListener = clickBoardImageListener;
    }

    public BoardAdapter(List<Board> boards) {
        this.boards = boards;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView macAddress;
        TextView identifier;
        TextView kbpub;
        Switch ledSwitch;
        ImageView ivBoard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            macAddress = itemView.findViewById(R.id.tv_mac_address);
            identifier = itemView.findViewById(R.id.tv_identifier);
            kbpub = itemView.findViewById(R.id.tv_kdpub);
            ledSwitch = itemView.findViewById(R.id.switch_led);
            ivBoard = itemView.findViewById(R.id.iv_board);
        }
    }

    @NonNull
    @Override
    public BoardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_board_node, viewGroup, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

        // 对图片进行
        holder.ivBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                // 获取当前点击的实例
                int position = holder.getAdapterPosition();

                clickBoardImageListener.onClickBoardImageListener(v, position);

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
