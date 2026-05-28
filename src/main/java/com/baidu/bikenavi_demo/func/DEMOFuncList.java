package com.baidu.bikenavi_demo.func;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.baidu.bikenavi_demo.R;

import java.util.ArrayList;
import java.util.List;

public class DEMOFuncList extends FrameLayout {

    private MyAdapter myAdapter;
    private final List<DEMOFuncBean> demoFuncBean = new ArrayList<>();
    private ExpandableListView expandableListView;

    public DEMOFuncList(Context context) {
        super(context);
        init(context);
    }

    public DEMOFuncList(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DEMOFuncList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context c) {
        LayoutInflater.from(c).inflate(R.layout.list_func, this);
        expandableListView = findViewById(R.id.elv_common_func);
        myAdapter = new MyAdapter();
        expandableListView.setAdapter(myAdapter);
    }

    public void setOnChildClickListener(ExpandableListView.OnChildClickListener listener) {
        expandableListView.setOnChildClickListener(listener);
    }

    public void setOnGroupClickListener(ExpandableListView.OnGroupClickListener listener) {
        expandableListView.setOnGroupClickListener(listener);
    }

    public void setData(List<DEMOFuncBean> data) {
        demoFuncBean.clear();
        demoFuncBean.addAll(data);
        myAdapter.notifyDataSetChanged();
    }

    public List<DEMOFuncBean> getData() {
        return demoFuncBean;
    }

    private class MyAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return demoFuncBean.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return demoFuncBean.get(groupPosition).childList.size();
        }

        @Override
        public DEMOFuncBean getGroup(int groupPosition) {
            return demoFuncBean.get(groupPosition);
        }

        @Override
        public DEMOFuncBean.DEMOFuncBeanChild getChild(int groupPosition, int childPosition) {
            return demoFuncBean.get(groupPosition).childList.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View inflate = LayoutInflater.from(getContext()).inflate(R.layout.item_func_parent, null);
            TextView viewById = inflate.findViewById(R.id.tv_group_name);
            viewById.setText(demoFuncBean.get(groupPosition).getFuncName());
            return inflate;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View inflate = LayoutInflater.from(getContext()).inflate(R.layout.item_func_child, null);
            TextView viewById = inflate.findViewById(R.id.tv_name);
            viewById.setText(demoFuncBean.get(groupPosition).childList.get(childPosition).getFuncName());
            return inflate;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
