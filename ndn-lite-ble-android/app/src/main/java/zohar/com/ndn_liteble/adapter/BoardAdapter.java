package zohar.com.ndn_liteble.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import zohar.com.ndn_liteble.R;
import zohar.com.ndn_liteble.model.Board;

public class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.ViewHolder> {

    private static final String TAG = "BoardAdapter";

    // 填充的数据
    private List<Board> boards;

    // 点击图片的监听事件
    private OnClickBoardImageListener clickBoardImageListener;
    // 点击switch的监听事件
    private OnClickSwitchListener onClickSwitchListener;
    // 点击radiogroup
    private OnCheckedChangeListener onCheckedChangeListener;

    /**
     * interface: switch开关监听事件
     */
    public interface OnClickSwitchListener{
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked, int position);
    }

    public void setOnClickSwitchListener(OnClickSwitchListener onClickSwitchListener){
        this.onClickSwitchListener = onClickSwitchListener;
    }

    // radiogroup
    public interface OnCheckedChangeListener{
        void onCheckedChanged(RadioGroup group, int checkedId, int position);
    }

    public void setOnCheckedChangeListener (OnCheckedChangeListener onCheckedChangeListener){
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    /**
     *
     * interface ： 图片点击监听事件
     */
    public interface OnClickBoardImageListener{
         void onClickBoardImageListener(View view, Board board);
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
        RadioGroup radioGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            macAddress = itemView.findViewById(R.id.tv_mac_address);
            identifier = itemView.findViewById(R.id.tv_identifier);
            kbpub = itemView.findViewById(R.id.tv_kdpub);
            ledSwitch = itemView.findViewById(R.id.switch_led);
            ivBoard = itemView.findViewById(R.id.iv_board);
            radioGroup = itemView.findViewById(R.id.rg_policy_select);
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
                // 获取当前点击的实例
                int position = holder.getAdapterPosition();
                onClickSwitchListener.onCheckedChanged(buttonView,isChecked,position);
            }
        });

        // 对图片进行
        holder.ivBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // 获取当前点击的实例
                int position = holder.getAdapterPosition();
                Board board = boards.get(position);

                clickBoardImageListener.onClickBoardImageListener(v, board);

            }
        });

        holder.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // 获取当前点击的实例
                int position = holder.getAdapterPosition();
                onCheckedChangeListener.onCheckedChanged(group,checkedId, position);
            }
        });

        return holder;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHoder, int i) {
        Board board = boards.get(i);
        if (board != null) {
            viewHoder.macAddress.setText(board.getMacAddress());
            viewHoder.identifier.setText(board.getIdentifierHex());
            viewHoder.kbpub.setText(board.getKDPubCertificate());
        }
    }

    @Override
    public int getItemCount() {
        return boards.size();
    }




}
