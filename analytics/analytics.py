import os
from datetime import datetime

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


def _save_plot(fig, output_dir, filename):
    os.makedirs(output_dir, exist_ok=True)
    path = os.path.join(output_dir, filename)
    fig.savefig(path, bbox_inches="tight", facecolor=fig.get_facecolor())
    plt.close(fig)
    return filename


def generate_admin_charts(db, output_dir="static/generated"):
    """Generate analytics charts for admin dashboard using pandas/numpy/matplotlib."""
    request_docs = list(db["requests"].find())
    df = pd.DataFrame(request_docs)

    timestamp = datetime.utcnow().strftime("%Y%m%d%H%M%S")

    if df.empty:
        category_counts = pd.Series([0], index=["No Data"])
        status_counts = pd.Series([0, 0, 1], index=["Completed", "In Progress", "Open"])
    else:
        category_counts = df["category"].fillna("Unknown").value_counts().sort_values(ascending=False)
        status_counts = df["status"].fillna("Open").value_counts()

    # Bar chart: number of help requests by category.
    fig1, ax1 = plt.subplots(figsize=(7, 4))
    fig1.patch.set_facecolor("#101b2d")
    ax1.set_facecolor("#101b2d")
    x_positions = np.arange(len(category_counts))
    ax1.bar(x_positions, category_counts.values, color="#2f6f8f")
    ax1.set_xticks(x_positions)
    ax1.set_xticklabels(category_counts.index, rotation=30, ha="right", color="#e5edff")
    ax1.set_title("Help Requests by Category", color="#e5edff")
    ax1.set_ylabel("Count", color="#e5edff")
    ax1.tick_params(axis="y", colors="#e5edff")
    for spine in ax1.spines.values():
        spine.set_color("#314a6d")
    category_file = _save_plot(fig1, output_dir, f"category_{timestamp}.png")

    # Pie chart: requests completed vs pending/accepted.
    fig2, ax2 = plt.subplots(figsize=(5, 5))
    fig2.patch.set_facecolor("#101b2d")
    ax2.set_facecolor("#101b2d")
    ordered_status = status_counts.reindex(["Completed", "In Progress", "Open"]).fillna(0)
    ax2.pie(
        ordered_status.values,
        labels=ordered_status.index,
        autopct="%1.0f%%",
        startangle=120,
        colors=["#22c55e", "#3b82f6", "#f59e0b"],
        textprops={"color": "#e5edff", "fontsize": 10},
    )
    ax2.set_title("Requests by Status", color="#e5edff")
    status_file = _save_plot(fig2, output_dir, f"status_{timestamp}.png")

    # Volunteer count as simple one-bar chart for quick monitoring.
    volunteer_count = db["users"].count_documents({"role": "volunteer"})
    fig3, ax3 = plt.subplots(figsize=(4, 4))
    fig3.patch.set_facecolor("#101b2d")
    ax3.set_facecolor("#101b2d")
    ax3.bar(["Active Volunteers"], [volunteer_count], color="#5cb85c")
    ax3.set_ylim(bottom=0)
    ax3.set_title("Volunteer Availability", color="#e5edff")
    ax3.tick_params(axis="x", colors="#e5edff")
    ax3.tick_params(axis="y", colors="#e5edff")
    for spine in ax3.spines.values():
        spine.set_color("#314a6d")
    volunteers_file = _save_plot(fig3, output_dir, f"volunteers_{timestamp}.png")

    return {
        "category_chart": category_file,
        "status_chart": status_file,
        "volunteers_chart": volunteers_file,
    }
