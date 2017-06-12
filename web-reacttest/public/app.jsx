

function FunctionalComponent(props) {
    return <p>[FunctionalComponent]</p>;
}

class SimpleComponent extends React.Component {

    render() {
        return <p>[SimpleComponent]</p>;
    }

}

class ParentComponent extends React.Component {

    render() {
        return <div style={ {width:"300px",border: "2px solid black"} }>
            <div style={ {backgroundColor:"black",color:"white",padding:"5px"} }>Parent</div>
            {this.props.children}
        </div>;
    }

}

class InteractiveComponent extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            value: this.props.initialValue,
        };
    }

    resetValue() {
        this.setState({value: this.props.initialValue});
    }

    render() {
        var valueLink = {
            value: this.state.value,
            requestChange: (newValue) => this.setState({value: newValue}),
        };
        return (
            <p>
                <button type="button" onClick={() => this.resetValue()}>Reset</button>
                &nbsp;
                <input type="text" valueLink={valueLink} />
                &nbsp;
                <span>{this.state.value}</span>
            </p>
        );
    }

}

InteractiveComponent.defaultProps = {
    initialValue : "initial value",
};

class AnimatedComponent extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            index: 0,
            forward: true,
        };
    }

    tick() {
        var newIndex;
        var newForward = this.state.forward;
        if (this.state.forward) {
            newIndex = this.state.index + 1;
            if (newIndex >= this.props.length) {
                newIndex -= 2;
                newForward = false;
            }
        } else {
            newIndex = this.state.index - 1;
            if (newIndex < 0) {
                newIndex = 1;
                newForward = true;
            }
        }
        this.setState({index: newIndex, forward: newForward});
    }

    componentDidMount() {
        var self = this;
        this.timer = setInterval(() => this.tick(), this.props.delay);
    }

    componentWillUnmount() {
        clearInterval(this.timer);
    }

    getContent(length, index) {
        return (
            "[ ]".repeat(index) +
            "[x]" +
            "[ ]".repeat(length - index - 1)
        );
    }

    render() {
        return <p style={ {fontFamily: "monospace"} }>
            {this.getContent(this.props.length, this.state.index)}</p>
        ;
    }


}

AnimatedComponent.defaultProps = {
    delay: 100,
    length: 10,
};

ReactDOM.render(
    <div>
        <h2>This is a react app</h2>
        <p>Rendered at: {new Date().toLocaleTimeString()}</p>
        <FunctionalComponent />
        <ParentComponent>
            <SimpleComponent />
        </ParentComponent>
        <InteractiveComponent />
        <AnimatedComponent />
        <AnimatedComponent length="2" delay="1000" />
    </div>,
    document.getElementById("app")
);
