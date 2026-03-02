"""pytorchexample: ClientApp that loads data via Armadillo push-data endpoint."""

import torch
from flwr.app import ArrayRecord, Context, Message, MetricRecord, RecordDict
from flwr.clientapp import ClientApp

from molgenis_flwr_armadillo import get_node_token, get_node_url
from pytorchexample.task import Net, get_dataloaders
from pytorchexample.task import test as test_fn
from pytorchexample.task import train as train_fn

app = ClientApp()


@app.train()
def train(msg: Message, context: Context):
    """Train the model on data loaded from Armadillo."""

    # Load the model and initialize with received weights
    model = Net()
    model.load_state_dict(msg.content["arrays"].to_torch_state_dict())
    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    model.to(device)

    # Get this node's Armadillo credentials from the message
    token = get_node_token(msg, context)
    url = get_node_url(msg, context)
    project = msg.content["config"]["project"]

    print(f"Loading data from {url} for project '{project}'...")
    trainloader, _ = get_dataloaders(url, token, project)

    # Train
    train_loss = train_fn(
        model,
        trainloader,
        context.run_config["local-epochs"],
        msg.content["config"]["lr"],
        device,
    )

    # Return trained weights
    model_record = ArrayRecord(model.state_dict())
    metrics = {
        "train_loss": train_loss,
        "num-examples": len(trainloader.dataset),
    }
    metric_record = MetricRecord(metrics)
    content = RecordDict({"arrays": model_record, "metrics": metric_record})
    return Message(content=content, reply_to=msg)


@app.evaluate()
def evaluate(msg: Message, context: Context):
    """Evaluate the model on data loaded from Armadillo."""

    model = Net()
    model.load_state_dict(msg.content["arrays"].to_torch_state_dict())
    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    model.to(device)

    # Get this node's Armadillo credentials from the message
    token = get_node_token(msg, context)
    url = get_node_url(msg, context)
    project = msg.content["config"]["project"]

    print(f"Loading eval data from {url} for project '{project}'...")
    _, valloader = get_dataloaders(url, token, project)

    eval_loss, eval_acc = test_fn(model, valloader, device)

    metrics = {
        "eval_loss": eval_loss,
        "eval_acc": eval_acc,
        "num-examples": len(valloader.dataset),
    }
    metric_record = MetricRecord(metrics)
    content = RecordDict({"metrics": metric_record})
    return Message(content=content, reply_to=msg)
