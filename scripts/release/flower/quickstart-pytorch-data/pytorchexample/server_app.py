"""pytorchexample: ServerApp that distributes tokens and URLs to clients."""

import torch
from flwr.app import ArrayRecord, ConfigRecord, Context
from flwr.serverapp import Grid, ServerApp
from flwr.serverapp.strategy import FedAvg

from molgenis_flwr_armadillo import extract_tokens
from pytorchexample.task import Net

app = ServerApp()


@app.main()
def main(grid: Grid, context: Context) -> None:
    """Main entry point for the ServerApp."""

    # Read run config
    fraction_train: float = context.run_config["fraction-train"]
    num_rounds: int = context.run_config["num-server-rounds"]
    lr: float = context.run_config["learning-rate"]
    project: str = context.run_config["project"]

    # Collect tokens and URLs from run_config for passing to clients
    tokens = extract_tokens(context)

    # Load global model
    global_model = Net()
    arrays = ArrayRecord(global_model.state_dict())

    # Initialize FedAvg strategy
    strategy = FedAvg(fraction_train=fraction_train)

    # Build configs: both train and evaluate need tokens + URLs + project
    train_config = ConfigRecord({"lr": lr, "project": project, **tokens})
    evaluate_config = ConfigRecord({"project": project, **tokens})

    # Start strategy
    result = strategy.start(
        grid=grid,
        initial_arrays=arrays,
        train_config=train_config,
        evaluate_config=evaluate_config,
        num_rounds=num_rounds,
    )

    # Check if any training actually happened
    if not result.train_metrics_clientapp:
        raise RuntimeError(
            "Federated learning failed: no successful training rounds. "
            "All client nodes returned errors. Check the clientapp logs for details."
        )

    # Save final model to disk
    print("\nSaving final model to disk...")
    state_dict = result.arrays.to_torch_state_dict()
    torch.save(state_dict, "final_model.pt")
